package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.memory.MemoryLinkRepository
import com.mneme.memory.MemoryRepository
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 메모리별 backlink 조회 — `GET /api/memories/{extId}/backlinks`.
 *
 * 타겟이 이 메모리인 다른 메모리의 source 메모리를 반환한다.
 *
 * @author Mneme
 * @since phase 17
 */
@RestController
@RequestMapping("/api/memories")
class BacklinksController(
    private val memoryRepository: MemoryRepository,
    private val memoryLinkRepository: MemoryLinkRepository,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 메모리의 backlink 목록. */
    @GetMapping("/{extId}/backlinks")
    fun backlinks(
        @PathVariable extId: String,
    ): List<BacklinkResponse> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        val links = memoryLinkRepository.findAllByUserIdAndTargetId(userId, memoryId)
        if (links.isEmpty()) return emptyList()
        val sourceIds = links.map { it.sourceId }.toSet()
        val sources =
            memoryRepository
                .findAllByUserIdAndArchivedAtIsNull(userId)
                .filter { it.id in sourceIds }
        return sources.map { m ->
            BacklinkResponse(
                extId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                title = m.title,
                summary = m.summary,
            )
        }
    }

    /** backlink 응답 DTO. */
    data class BacklinkResponse(
        val extId: String,
        val title: String,
        val summary: String?,
    )
}
