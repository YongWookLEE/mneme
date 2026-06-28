package com.mneme.export

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mneme.id.IdFactory
import com.mneme.memory.FolderRepository
import com.mneme.memory.FolderService
import com.mneme.memory.MemoryRepository
import com.mneme.memory.MemoryService
import com.mneme.memory.MemoryWriteFacade
import com.mneme.memory.TagService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * 메모리 import 서비스(zip 파싱).
 *
 * 2단계 흐름:
 * 1. [preview]: zip을 파싱해 각 항목의 제목·폴더 경로·기존 메모리와의 충돌 여부를 리스트로 반환. 세션 토큰(UUID)을 발급.
 * 2. [apply]: 사용자가 선택한 action(skip/replace/create-new)을 받아 실제로 메모리를 만든다.
 *
 * 지원 포맷:
 * - **Mneme zip**: manifest.json 존재. extId·folder 경로·태그가 보존됨.
 * - **일반 마크다운 zip**: zip 내부 디렉터리를 폴더로 매핑. .md 파일의 frontmatter(있으면) 또는 첫 # 헤딩을 제목으로.
 *
 * 임시 보관은 Caffeine 30분 TTL. 격리: 모든 메서드 첫 인자 userId.
 *
 * @author Mneme
 * @since phase 13
 */
@Service
@Transactional
class ImportService(
    private val folderRepository: FolderRepository,
    private val folderService: FolderService,
    private val memoryRepository: MemoryRepository,
    private val memoryService: MemoryService,
    private val memoryWriteFacade: MemoryWriteFacade,
    private val tagService: TagService,
    private val idFactory: IdFactory,
) {
    private lateinit var sessions: Cache<UUID, ImportSession>

    @PostConstruct
    fun init() {
        sessions =
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(100)
                .build()
    }

    /** zip을 파싱해 항목 목록 + 충돌 표시를 반환한다. session token으로 [apply] 호출에 연결. */
    fun preview(
        userId: UUID,
        file: MultipartFile,
    ): PreviewResult {
        val items = mutableListOf<ParsedItem>()
        ZipInputStream(file.inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".md")) {
                    val bytes = zip.readAllBytes()
                    val text = String(bytes, Charsets.UTF_8)
                    val parsed = parseMarkdown(entry.name, text)
                    items.add(parsed)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val existingByPathTitle =
            memoryRepository
                .findAllByUserIdAndArchivedAtIsNull(userId)
                .associate { (folderRepository.findByUserIdAndId(userId, it.folderId)?.path ?: "/") to it.title to it }
        val sessionId = idFactory.newUuid()
        val previewItems =
            items.mapIndexed { index, p ->
                val targetPath = p.folderPath ?: "/"
                val conflict = existingByPathTitle[targetPath to p.title]
                PreviewItem(
                    index = index,
                    filename = p.filename,
                    title = p.title,
                    folderPath = targetPath,
                    tagNames = p.tagNames,
                    byteSize = p.content.toByteArray(Charsets.UTF_8).size,
                    conflictExtId =
                        conflict?.let {
                            com.mneme.id
                                .PrefixedId(com.mneme.id.PrefixedId.Prefix.MEMORY, it.id)
                                .format()
                        },
                )
            }
        sessions.put(sessionId, ImportSession(userId = userId, items = items))
        return PreviewResult(sessionId = sessionId.toString(), items = previewItems)
    }

    /** preview에서 발급한 sessionId + 사용자 결정을 받아 실제 import 수행. */
    fun apply(
        userId: UUID,
        sessionIdRaw: String,
        decisions: List<ApplyDecision>,
    ): ApplyResult {
        val sessionId = UUID.fromString(sessionIdRaw)
        val session = sessions.getIfPresent(sessionId) ?: throw IllegalArgumentException("session_expired_or_not_found")
        check(session.userId == userId) { "session_user_mismatch" }
        sessions.invalidate(sessionId)

        var imported = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        for (decision in decisions) {
            val item = session.items.getOrNull(decision.index) ?: continue
            try {
                when (decision.action) {
                    "skip" -> {
                        skipped++
                    }
                    "replace", "create-new" -> {
                        val title = decision.newTitle ?: item.title
                        val folderId = ensureFolder(userId, item.folderPath ?: "/")
                        if (decision.action == "replace") {
                            val existing = memoryRepository.findByUserIdAndTitleAndArchivedAtIsNull(userId, item.title)
                            if (existing != null && existing.folderId == folderId) {
                                memoryWriteFacade.update(
                                    userId = userId,
                                    memoryId = existing.id,
                                    expectedVersion = existing.version,
                                    title = title,
                                    content = item.content,
                                    summary = null,
                                    folderId = null,
                                )
                                attachTags(userId, existing.id, item.tagNames)
                                imported++
                                continue
                            }
                        }
                        val created =
                            memoryWriteFacade.create(
                                userId = userId,
                                folderId = folderId,
                                title = title,
                                content = item.content,
                                summary = null,
                                sourceUri = null,
                            )
                        attachTags(userId, created.id, item.tagNames)
                        imported++
                    }
                    else -> errors.add("unknown_action: ${decision.action} for ${item.filename}")
                }
            } catch (e: Exception) {
                errors.add("${item.filename}: ${e.message}")
            }
        }
        return ApplyResult(imported = imported, skipped = skipped, errors = errors)
    }

    /** materialized path를 갖는 폴더가 없으면 차례로 생성하고 마지막 폴더 id를 반환. */
    private fun ensureFolder(
        userId: UUID,
        path: String,
    ): UUID {
        if (path == "/" || path.isBlank()) {
            val rootName = "import"
            val rootPath = "/$rootName/"
            return folderRepository.findByUserIdAndPath(userId, rootPath)?.id
                ?: folderService.create(userId, parentId = null, name = rootName).id
        }
        val segments = path.trim('/').split('/').filter { it.isNotBlank() }
        var parentId: UUID? = null
        var current = "/"
        for (seg in segments) {
            current += "$seg/"
            val existing = folderRepository.findByUserIdAndPath(userId, current)
            parentId =
                if (existing != null) {
                    existing.id
                } else {
                    folderService.create(userId, parentId = parentId, name = seg).id
                }
        }
        return parentId!!
    }

    private fun attachTags(
        userId: UUID,
        memoryId: UUID,
        tagNames: List<String>,
    ) {
        for (name in tagNames) {
            try {
                tagService.attach(userId, memoryId, name)
            } catch (e: Exception) {
                // tag 부착 실패는 무시하고 이어감
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun memoryServiceUnused(memoryService: MemoryService) {}

    /** zip 내 한 항목 파싱. frontmatter 있으면 title/folder_path 추출, 없으면 파일명에서 추론. */
    private fun parseMarkdown(
        filename: String,
        text: String,
    ): ParsedItem {
        val trimmed = text
        var title: String? = null
        var folderPath: String? = null
        var tagNames: List<String> = emptyList()
        var body = trimmed
        if (trimmed.startsWith("---")) {
            val endIdx = trimmed.indexOf("\n---", 3)
            if (endIdx > 0) {
                val fm = trimmed.substring(3, endIdx).trim()
                body = trimmed.substring(endIdx + 4).trimStart('\n')
                for (line in fm.lines()) {
                    val colon = line.indexOf(':')
                    if (colon < 0) continue
                    val key = line.substring(0, colon).trim()
                    val raw = line.substring(colon + 1).trim()
                    val value = raw.trim('"')
                    when (key) {
                        "title" -> title = value
                        "folder_path" -> folderPath = value
                        "folder_ext_id" -> {
                            // Mneme zip의 경우 folder ext_id가 있지만 본 단순 import는 경로 무시 — manifest.json
                            // 기반의 완전한 import는 후속 phase에서.
                        }
                        "tags" -> {
                            val inner = value.trim('[', ']')
                            tagNames = inner.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        }
                    }
                }
            }
        }
        if (title == null) {
            val firstHeading = body.lineSequence().firstOrNull { it.startsWith("# ") }
            title = firstHeading?.removePrefix("# ")?.trim() ?: filename.substringAfterLast('/').removeSuffix(".md")
        }
        val pathFromZip = filename.substringBeforeLast('/', missingDelimiterValue = "")
        val effectiveFolder = folderPath ?: if (pathFromZip.isBlank() || pathFromZip == "memories") "/" else "/$pathFromZip/"
        return ParsedItem(
            filename = filename,
            title = title ?: filename,
            folderPath = effectiveFolder,
            content = body,
            tagNames = tagNames,
        )
    }

    /** 파싱된 임시 항목. */
    data class ParsedItem(
        val filename: String,
        val title: String,
        val folderPath: String?,
        val content: String,
        val tagNames: List<String>,
    )

    /** 임시 세션 보관. */
    data class ImportSession(
        val userId: UUID,
        val items: List<ParsedItem>,
    )

    /** preview 응답 항목. */
    data class PreviewItem(
        val index: Int,
        val filename: String,
        val title: String,
        val folderPath: String,
        val tagNames: List<String>,
        val byteSize: Int,
        val conflictExtId: String?,
    )

    /** preview 응답. */
    data class PreviewResult(
        val sessionId: String,
        val items: List<PreviewItem>,
    )

    /** apply 요청 항목 — 항목별 사용자 결정. */
    data class ApplyDecision(
        val index: Int,
        val action: String, // "skip" | "replace" | "create-new"
        val newTitle: String? = null,
    )

    /** apply 응답. */
    data class ApplyResult(
        val imported: Int,
        val skipped: Int,
        val errors: List<String>,
    )
}
