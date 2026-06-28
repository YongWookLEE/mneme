package com.mneme.mcp

import com.mneme.auth.UserRepository
import com.mneme.id.PrefixedId
import com.mneme.memory.FolderService
import com.mneme.memory.Memory
import com.mneme.memory.MemoryLinkRepository
import com.mneme.memory.MemoryRepository
import com.mneme.memory.MemoryService
import com.mneme.memory.MemoryWriteFacade
import com.mneme.memory.TagService
import com.mneme.search.SearchFilter
import com.mneme.search.SearchService
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/**
 * MCP `mn_*` 11개 도구의 Kotlin 구현체.
 *
 * 각 메서드는 [AuthenticatedUserResolver]로 현재 사용자 ID를 추출하고, 도메인 서비스로 위임한다.
 * 외부 ID는 모두 `<prefix>_<base32>` 포맷(`mem_`/`fld_`/`tag_`). 내부 UUID는 노출하지 않는다.
 *
 * Bearer 인증은 기존 `ApiKeyAuthenticationFilter`가 처리한다(MCP 엔드포인트도 동일 Spring Security 체인).
 * 다른 사용자 리소스 접근은 모두 404로 매핑(ADR-009 격리 정책).
 *
 * @author Mneme
 * @since phase 09
 */
@Component
class MnemeTools(
    private val userResolver: AuthenticatedUserResolver,
    private val userRepository: UserRepository,
    private val memoryService: MemoryService,
    private val memoryWriteFacade: MemoryWriteFacade,
    private val folderService: FolderService,
    private val tagService: TagService,
    private val searchService: SearchService,
    private val memoryRepository: MemoryRepository,
    private val memoryLinkRepository: MemoryLinkRepository,
) {
    /** 도구 카탈로그 + 서버 메타데이터. 디버깅·LLM 자기 점검용. */
    @Tool(
        name = "mn_schema",
        description = "List all Mneme MCP tools and server metadata. Use this to discover capabilities before calling other tools.",
    )
    fun schema(): Map<String, Any?> =
        mapOf(
            "server" to "mneme",
            "version" to SERVER_VERSION,
            "tools" to TOOL_CATALOG,
            "id_format" to
                mapOf(
                    "memory" to "mem_<base32>",
                    "folder" to "fld_<base32>",
                    "tag" to "tag_<name>",
                ),
            "content_limits" to
                mapOf(
                    "memory_body_max_bytes" to MemoryService.MAX_CONTENT_BYTES,
                ),
        )

    /** 현재 인증된 사용자 정보. */
    @Tool(
        name = "mn_whoami",
        description = "Return the authenticated user's external id, email and locale.",
    )
    fun whoami(): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val user =
            userRepository.findById(userId).orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED)
            }
        return mapOf(
            "user_ext_id" to PrefixedId(PrefixedId.Prefix.USER, user.id).format(),
            "email" to user.email,
            "locale" to user.locale,
        )
    }

    /**
     * 폴더 또는 폴더 내 메모리 목록. folder_ext_id 생략 시 모든 폴더 + 사용자 본인의 활성 메모리 평탄 목록.
     * include_archived=true면 archived 메모리도 포함.
     */
    @Tool(
        name = "mn_list",
        description =
            "List folders and memories. If folder_ext_id is provided, list memories inside that folder. " +
                "Otherwise, return all folders and the user's recent memories.",
    )
    fun list(
        @ToolParam(required = false, description = "Optional folder external id (fld_...). If null, all folders are returned.")
        folderExtId: String? = null,
        @ToolParam(required = false, description = "Include archived memories in the result. Default false.")
        includeArchived: Boolean? = null,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val folders = folderService.listAll(userId).map(::folderToMap)
        val memories =
            if (folderExtId != null) {
                val folderId = PrefixedId.parse(folderExtId, PrefixedId.Prefix.FOLDER).uuid
                folderService.get(userId, folderId)
                memoryRepository.findAllByUserIdAndFolderIdAndArchivedAtIsNull(userId, folderId)
            } else if (includeArchived == true) {
                memoryService.listActive(userId) + memoryService.listArchived(userId)
            } else {
                memoryService.listActive(userId)
            }
        return mapOf(
            "folders" to folders,
            "memories" to memories.map(::memorySummaryToMap),
        )
    }

    /** 단건 메모리(본문 포함) 조회. */
    @Tool(
        name = "mn_read",
        description = "Read a single memory by external id, including the full markdown body.",
    )
    fun read(
        @ToolParam(description = "Memory external id (mem_...).")
        memoryExtId: String,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(memoryExtId, PrefixedId.Prefix.MEMORY).uuid
        val memory = memoryService.get(userId, memoryId)
        val tags = tagService.listForMemory(userId, memoryId).map { it.name }
        return memoryFullToMap(memory) + mapOf("tags" to tags)
    }

    /** 하이브리드 검색. */
    @Tool(
        name = "mn_search",
        description =
            "Hybrid semantic + text search across the user's memories. " +
                "Returns ranked summaries (no full body). Use mn_read for content.",
    )
    fun search(
        @ToolParam(description = "Search query in natural language.")
        query: String,
        @ToolParam(required = false, description = "Optional folder external id to restrict the search.")
        folderExtId: String? = null,
        @ToolParam(
            required = false,
            description = "Optional tag names (lowercase) to restrict the search. Memory must have ALL listed tags.",
        )
        tags: List<String>? = null,
        @ToolParam(required = false, description = "Max number of results (1..100). Default 20.")
        limit: Int? = null,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val filter =
            SearchFilter(
                folderId = folderExtId?.let { PrefixedId.parse(it, PrefixedId.Prefix.FOLDER).uuid },
                tagNames = tags ?: emptyList(),
            )
        val hits = searchService.search(userId, query, filter, limit ?: DEFAULT_SEARCH_LIMIT)
        return mapOf(
            "hits" to
                hits.map { hit ->
                    mapOf(
                        "memory_ext_id" to PrefixedId(PrefixedId.Prefix.MEMORY, hit.id).format(),
                        "folder_ext_id" to PrefixedId(PrefixedId.Prefix.FOLDER, hit.folderId).format(),
                        "title" to hit.title,
                        "summary" to hit.summary,
                        "score" to hit.score,
                        "created_at" to hit.createdAt.toString(),
                        "updated_at" to hit.updatedAt.toString(),
                    )
                },
        )
    }

    /** 메모리 생성. */
    @Tool(
        name = "mn_write",
        description = "Create a new memory. Body supports markdown including [[wiki-links]] which are auto-indexed.",
    )
    fun write(
        @ToolParam(description = "Folder external id (fld_...) where the memory will be created.")
        folderExtId: String,
        @ToolParam(description = "Memory title (non-empty).")
        title: String,
        @ToolParam(description = "Markdown body (<= 256KB).")
        content: String,
        @ToolParam(required = false, description = "Optional short summary (1-2 lines). If null, LLM generates one.")
        summary: String? = null,
        @ToolParam(required = false, description = "Optional source URI (URL or local path) for provenance.")
        sourceUri: String? = null,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(folderExtId, PrefixedId.Prefix.FOLDER).uuid
        val memory = memoryWriteFacade.create(userId, folderId, title, content, summary, sourceUri)
        return memoryFullToMap(memory)
    }

    /** 메모리 갱신. 낙관적 락 version 필수. */
    @Tool(
        name = "mn_update",
        description =
            "Update an existing memory. The expected_version must match the current version " +
                "(optimistic locking, 409 on conflict).",
    )
    fun update(
        @ToolParam(description = "Memory external id (mem_...).")
        memoryExtId: String,
        @ToolParam(description = "Expected current version (from mn_read).")
        expectedVersion: Long,
        @ToolParam(required = false, description = "New title.")
        title: String? = null,
        @ToolParam(required = false, description = "New markdown body.")
        content: String? = null,
        @ToolParam(required = false, description = "New summary.")
        summary: String? = null,
        @ToolParam(required = false, description = "Move to a different folder (fld_...).")
        folderExtId: String? = null,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(memoryExtId, PrefixedId.Prefix.MEMORY).uuid
        val folderId = folderExtId?.let { PrefixedId.parse(it, PrefixedId.Prefix.FOLDER).uuid }
        val memory = memoryWriteFacade.update(userId, memoryId, expectedVersion, title, content, summary, folderId)
        return memoryFullToMap(memory)
    }

    /** Archive(soft delete). */
    @Tool(
        name = "mn_archive",
        description = "Archive a memory (soft delete). Restore with mn_restore. Permanent deletion is not supported.",
    )
    fun archive(
        @ToolParam(description = "Memory external id (mem_...).")
        memoryExtId: String,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(memoryExtId, PrefixedId.Prefix.MEMORY).uuid
        memoryService.archive(userId, memoryId)
        return mapOf("memory_ext_id" to memoryExtId, "archived" to true)
    }

    /** Archive 해제. */
    @Tool(
        name = "mn_restore",
        description = "Restore an archived memory. Same folder; conflicts on duplicate active title return 409.",
    )
    fun restore(
        @ToolParam(description = "Memory external id (mem_...).")
        memoryExtId: String,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(memoryExtId, PrefixedId.Prefix.MEMORY).uuid
        memoryService.restore(userId, memoryId)
        return mapOf("memory_ext_id" to memoryExtId, "archived" to false)
    }

    /**
     * 메모리 관계(본문 `[[wiki-link]]` 파생 인덱스) 조회. forward/backlinks/broken 분리.
     * 본문 파서는 phase 16에서 구현. 본 phase에서는 인덱스 조회만 노출.
     */
    @Tool(
        name = "mn_relations",
        description = "Get the wiki-link relations of a memory: outgoing links, backlinks, and broken links (target not found).",
    )
    fun relations(
        @ToolParam(description = "Memory external id (mem_...).")
        memoryExtId: String,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(memoryExtId, PrefixedId.Prefix.MEMORY).uuid
        memoryService.get(userId, memoryId)
        val forward = memoryLinkRepository.findAllByUserIdAndSourceId(userId, memoryId)
        val back = memoryLinkRepository.findAllByUserIdAndTargetId(userId, memoryId)
        return mapOf(
            "memory_ext_id" to memoryExtId,
            "forward" to forward.map(::linkToMap),
            "backlinks" to back.map(::linkToMap),
            "broken" to forward.filter { it.targetId == null }.map(::linkToMap),
        )
    }

    /**
     * 컨텍스트 힌트(자유 텍스트)로 관련 메모리를 추천한다. 내부적으로 하이브리드 검색의 wrapper.
     * LLM이 현재 대화 주제와 관련된 기억을 능동 호출할 때 쓰는 진입점.
     */
    @Tool(
        name = "mn_surface",
        description =
            "Surface memories likely relevant to a free-text context hint. " +
                "Lighter than mn_search; intended for proactive recall.",
    )
    fun surface(
        @ToolParam(description = "Free-text hint describing the current conversation context.")
        contextHint: String,
        @ToolParam(required = false, description = "Max number of suggestions (1..20). Default 5.")
        limit: Int? = null,
    ): Map<String, Any?> {
        val userId = userResolver.currentUserId()
        val effective = (limit ?: SURFACE_DEFAULT_LIMIT).coerceIn(1, SURFACE_MAX_LIMIT)
        val hits = searchService.search(userId, contextHint, SearchFilter(), effective)
        return mapOf(
            "suggestions" to
                hits.map { hit ->
                    mapOf(
                        "memory_ext_id" to PrefixedId(PrefixedId.Prefix.MEMORY, hit.id).format(),
                        "title" to hit.title,
                        "summary" to hit.summary,
                        "score" to hit.score,
                    )
                },
        )
    }

    private fun folderToMap(folder: com.mneme.memory.Folder): Map<String, Any?> =
        mapOf(
            "folder_ext_id" to PrefixedId(PrefixedId.Prefix.FOLDER, folder.id).format(),
            "parent_ext_id" to folder.parentId?.let { PrefixedId(PrefixedId.Prefix.FOLDER, it).format() },
            "name" to folder.name,
            "path" to folder.path,
        )

    private fun memorySummaryToMap(memory: Memory): Map<String, Any?> =
        mapOf(
            "memory_ext_id" to PrefixedId(PrefixedId.Prefix.MEMORY, memory.id).format(),
            "folder_ext_id" to PrefixedId(PrefixedId.Prefix.FOLDER, memory.folderId).format(),
            "title" to memory.title,
            "summary" to memory.summary,
            "created_at" to memory.createdAt.toString(),
            "updated_at" to memory.updatedAt.toString(),
            "archived_at" to memory.archivedAt?.toString(),
            "version" to memory.version,
        )

    private fun memoryFullToMap(memory: Memory): Map<String, Any?> =
        memorySummaryToMap(memory) +
            mapOf(
                "content" to memory.content,
                "source_uri" to memory.sourceUri,
            )

    private fun linkToMap(link: com.mneme.memory.MemoryLink): Map<String, Any?> =
        mapOf(
            "source_ext_id" to PrefixedId(PrefixedId.Prefix.MEMORY, link.sourceId).format(),
            "target_ext_id" to link.targetId?.let { PrefixedId(PrefixedId.Prefix.MEMORY, it).format() },
            "target_label" to link.targetLabel,
            "kind" to link.kind,
        )

    /** archive/restore 응답에서 사용하는 보조 캐스팅 — Kotlin smartcast 보조 (사용 안함, 미래 확장). */
    @Suppress("unused")
    private fun nowIso(): String = OffsetDateTime.now().toString()

    companion object {
        private const val SERVER_VERSION = "0.1.0"
        private const val DEFAULT_SEARCH_LIMIT = 20
        private const val SURFACE_DEFAULT_LIMIT = 5
        private const val SURFACE_MAX_LIMIT = 20

        private val TOOL_CATALOG =
            listOf(
                "mn_schema" to "List tool catalog and server metadata.",
                "mn_whoami" to "Return authenticated user info.",
                "mn_list" to "List folders or memories in a folder.",
                "mn_read" to "Read a single memory with full body.",
                "mn_search" to "Hybrid search across memories.",
                "mn_write" to "Create a memory.",
                "mn_update" to "Update a memory (optimistic locking).",
                "mn_archive" to "Archive (soft delete) a memory.",
                "mn_restore" to "Restore an archived memory.",
                "mn_relations" to "Get wiki-link forward/backlinks/broken.",
                "mn_surface" to "Surface memories relevant to a context hint.",
            ).map { (name, desc) -> mapOf("name" to name, "description" to desc) }

        /** 의도하지 않은 UUID 노출을 막기 위한 보조(테스트 가독성). */
        @Suppress("unused")
        fun extMemory(id: UUID): String = PrefixedId(PrefixedId.Prefix.MEMORY, id).format()
    }
}
