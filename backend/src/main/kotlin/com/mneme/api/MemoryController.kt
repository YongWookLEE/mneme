package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.memory.Memory
import com.mneme.memory.MemoryService
import com.mneme.memory.MemoryWriteFacade
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * 메모리 REST 컨트롤러(코어 CRUD).
 *
 * 외부 ID = `mem_<base32>`. 본문 256KB 상한. 낙관적 락은 `version` 필드로 클라이언트에서 송신.
 */
@RestController
@RequestMapping("/api/memories")
class MemoryController(
    private val memoryService: MemoryService,
    private val memoryWriteFacade: MemoryWriteFacade,
    private val userResolver: AuthenticatedUserResolver,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody body: CreateRequest,
    ): MemoryResponse {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(body.folderExtId, PrefixedId.Prefix.FOLDER).uuid
        return toResponse(
            memoryWriteFacade.create(
                userId = userId,
                folderId = folderId,
                title = body.title,
                content = body.content,
                summary = body.summary,
                sourceUri = body.sourceUri,
            ),
        )
    }

    @GetMapping("/{extId}")
    fun get(
        @PathVariable extId: String,
    ): MemoryResponse {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        return toResponse(memoryService.get(userId, memoryId))
    }

    @PatchMapping("/{extId}")
    fun update(
        @PathVariable extId: String,
        @RequestBody body: UpdateRequest,
    ): MemoryResponse {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        val folderId = body.folderExtId?.let { PrefixedId.parse(it, PrefixedId.Prefix.FOLDER).uuid }
        return toResponse(
            memoryWriteFacade.update(
                userId = userId,
                memoryId = memoryId,
                expectedVersion = body.version,
                title = body.title,
                content = body.content,
                summary = body.summary,
                folderId = folderId,
            ),
        )
    }

    @PostMapping("/{extId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archive(
        @PathVariable extId: String,
    ) {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        memoryService.archive(userId, memoryId)
    }

    @PostMapping("/{extId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restore(
        @PathVariable extId: String,
    ) {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        memoryService.restore(userId, memoryId)
    }

    @GetMapping
    fun list(
        @RequestParam(name = "archived", required = false, defaultValue = "false") archived: Boolean,
    ): List<MemoryResponse> {
        val userId = userResolver.currentUserId()
        val list = if (archived) memoryService.listArchived(userId) else memoryService.listActive(userId)
        return list.map { toResponse(it) }
    }

    private fun toResponse(memory: Memory): MemoryResponse =
        MemoryResponse(
            extId = PrefixedId(PrefixedId.Prefix.MEMORY, memory.id).format(),
            folderExtId = PrefixedId(PrefixedId.Prefix.FOLDER, memory.folderId).format(),
            title = memory.title,
            content = memory.content,
            summary = memory.summary,
            sourceUri = memory.sourceUri,
            byteSize = memory.byteSize,
            archivedAt = memory.archivedAt,
            createdAt = memory.createdAt,
            updatedAt = memory.updatedAt,
            version = memory.version,
        )

    data class CreateRequest(
        val folderExtId: String,
        val title: String,
        val content: String,
        val summary: String?,
        val sourceUri: String?,
    )

    data class UpdateRequest(
        val version: Long,
        val title: String?,
        val content: String?,
        val summary: String?,
        val folderExtId: String?,
    )

    data class MemoryResponse(
        val extId: String,
        val folderExtId: String,
        val title: String,
        val content: String,
        val summary: String?,
        val sourceUri: String?,
        val byteSize: Int,
        val archivedAt: OffsetDateTime?,
        val createdAt: OffsetDateTime,
        val updatedAt: OffsetDateTime,
        val version: Long,
    )
}
