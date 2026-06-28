package com.mneme.export

import com.fasterxml.jackson.databind.ObjectMapper
import com.mneme.id.PrefixedId
import com.mneme.memory.FolderRepository
import com.mneme.memory.MemoryRepository
import com.mneme.memory.MemoryTagRepository
import com.mneme.memory.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 사용자 본인의 전체 메모리·폴더·태그를 zip으로 export.
 *
 * 패키지 구조:
 * - `manifest.json` — 메모리/폴더/태그 메타데이터 + Mneme 버전 + export 시각
 * - `memories/<memoryExtId>.md` — 마크다운 본문 (frontmatter에 title/folderExtId/tags)
 *
 * 격리: 모든 조회 첫 인자 userId. Streaming(zip을 메모리에 통째로 올리지 않음).
 *
 * @author Mneme
 * @since phase 13
 */
@Service
class ExportService(
    private val memoryRepository: MemoryRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
    private val memoryTagRepository: MemoryTagRepository,
    private val objectMapper: ObjectMapper,
) {
    /** 사용자 메모리 전체를 zip 스트림으로 쓴다. */
    @Transactional(readOnly = true)
    fun exportToStream(
        userId: UUID,
        out: OutputStream,
    ) {
        val folders = folderRepository.findAllByUserId(userId)
        val foldersByExtId = folders.associate { it.id to PrefixedId(PrefixedId.Prefix.FOLDER, it.id).format() }
        val tags = tagRepository.findAllByUserId(userId)
        val tagsByExtId = tags.associate { it.id to "tag_${it.name}" }
        val memories =
            memoryRepository.findAllByUserIdAndArchivedAtIsNull(userId) +
                memoryRepository.findAllByUserIdAndArchivedAtIsNotNull(userId)

        val manifest =
            Manifest(
                schema = "mneme-export-v1",
                exportedAt =
                    java.time.OffsetDateTime
                        .now()
                        .toString(),
                folders =
                    folders.map { f ->
                        FolderEntry(
                            extId = foldersByExtId[f.id]!!,
                            parentExtId = f.parentId?.let { foldersByExtId[it] },
                            name = f.name,
                            path = f.path,
                        )
                    },
                tags = tags.map { TagEntry(extId = tagsByExtId[it.id]!!, name = it.name) },
                memories =
                    memories.map { m ->
                        val tagIds = memoryTagRepository.findAllByIdMemoryId(m.id).map { it.id.tagId }.toSet()
                        val tagNames = tags.filter { it.id in tagIds }.map { it.name }
                        MemoryEntry(
                            extId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                            folderExtId = foldersByExtId[m.folderId]!!,
                            title = m.title,
                            summary = m.summary,
                            sourceUri = m.sourceUri,
                            tagNames = tagNames,
                            archived = m.archivedAt != null,
                            createdAt = m.createdAt.toString(),
                            updatedAt = m.updatedAt.toString(),
                            byteSize = m.byteSize,
                        )
                    },
            )

        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest))
            zip.closeEntry()

            for (m in memories) {
                val ext = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format()
                val tagNames =
                    tags
                        .filter {
                            memoryTagRepository.findAllByIdMemoryId(m.id).any { mt -> mt.id.tagId == it.id }
                        }.map { it.name }
                val frontmatter =
                    buildString {
                        appendLine("---")
                        appendLine("title: ${escapeYaml(m.title)}")
                        appendLine("ext_id: $ext")
                        appendLine("folder_ext_id: ${foldersByExtId[m.folderId]}")
                        if (m.summary != null) appendLine("summary: ${escapeYaml(m.summary!!)}")
                        if (tagNames.isNotEmpty()) appendLine("tags: [${tagNames.joinToString(", ")}]")
                        if (m.archivedAt != null) appendLine("archived: true")
                        appendLine("created_at: ${m.createdAt}")
                        appendLine("updated_at: ${m.updatedAt}")
                        appendLine("---")
                    }
                zip.putNextEntry(ZipEntry("memories/$ext.md"))
                zip.write(frontmatter.toByteArray(Charsets.UTF_8))
                zip.write("\n".toByteArray(Charsets.UTF_8))
                zip.write(m.content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun escapeYaml(s: String): String {
        val needsQuotes = s.contains(":") || s.contains("#") || s.contains("\"")
        return if (needsQuotes) "\"${s.replace("\"", "\\\"")}\"" else s
    }

    /** Manifest 직렬화 모델. */
    data class Manifest(
        val schema: String,
        val exportedAt: String,
        val folders: List<FolderEntry>,
        val tags: List<TagEntry>,
        val memories: List<MemoryEntry>,
    )

    data class FolderEntry(
        val extId: String,
        val parentExtId: String?,
        val name: String,
        val path: String,
    )

    data class TagEntry(
        val extId: String,
        val name: String,
    )

    data class MemoryEntry(
        val extId: String,
        val folderExtId: String,
        val title: String,
        val summary: String?,
        val sourceUri: String?,
        val tagNames: List<String>,
        val archived: Boolean,
        val createdAt: String,
        val updatedAt: String,
        val byteSize: Int,
    )
}
