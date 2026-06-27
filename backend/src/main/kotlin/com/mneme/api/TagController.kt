package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.memory.Tag
import com.mneme.memory.TagService
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 태그 REST 컨트롤러.
 *
 * - GET /api/tags — 사용자 본인의 모든 태그
 * - GET /api/memories/{extId}/tags — 특정 메모리의 태그 목록
 * - POST /api/memories/{extId}/tags — 태그 부착(이름)
 * - DELETE /api/memories/{extId}/tags/{tagExtId} — 분리
 */
@RestController
@RequestMapping
class TagController(
    private val tagService: TagService,
    private val userResolver: AuthenticatedUserResolver,
) {
    @GetMapping("/api/tags")
    fun listAll(): List<TagResponse> {
        val userId = userResolver.currentUserId()
        return tagService.listAll(userId).map { toResponse(it) }
    }

    @GetMapping("/api/memories/{extId}/tags")
    fun listForMemory(
        @PathVariable extId: String,
    ): List<TagResponse> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        return tagService.listForMemory(userId, memoryId).map { toResponse(it) }
    }

    @PostMapping("/api/memories/{extId}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    fun attach(
        @PathVariable extId: String,
        @RequestBody body: AttachRequest,
    ): TagResponse {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        return toResponse(tagService.attach(userId, memoryId, body.name))
    }

    @DeleteMapping("/api/memories/{extId}/tags/{tagExtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun detach(
        @PathVariable extId: String,
        @PathVariable tagExtId: String,
    ) {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        val tagId = PrefixedId.parse(tagExtId, PrefixedId.Prefix.TAG).uuid
        tagService.detach(userId, memoryId, tagId)
    }

    private fun toResponse(tag: Tag): TagResponse =
        TagResponse(
            extId = PrefixedId(PrefixedId.Prefix.TAG, tag.id).format(),
            name = tag.name,
        )

    data class AttachRequest(
        val name: String,
    )

    data class TagResponse(
        val extId: String,
        val name: String,
    )
}
