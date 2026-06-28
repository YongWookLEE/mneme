package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.security.AuthenticatedUserResolver
import com.mneme.wiki.FeedbackService
import com.mneme.wiki.MemoryFeedback
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * 메모리 피드백 REST.
 *
 * - `POST /api/memories/{ext}/feedback` — 본인 피드백 저장.
 * - `GET /api/memories/{ext}/feedback` — 본인이 이 메모리에 남긴 피드백 최신순.
 *
 * @author Mneme
 * @since phase 23
 */
@RestController
@RequestMapping("/api/memories")
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 피드백 저장. */
    @PostMapping("/{extId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(
        @PathVariable extId: String,
        @RequestBody body: SubmitRequest,
    ): FeedbackResponse {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        val saved = feedbackService.submit(userId, memoryId, body.target, body.value, body.note)
        return saved.toResponse(extId)
    }

    /** 특정 메모리 피드백 목록. */
    @GetMapping("/{extId}/feedback")
    fun list(
        @PathVariable extId: String,
    ): List<FeedbackResponse> {
        val userId = userResolver.currentUserId()
        val memoryId = PrefixedId.parse(extId, PrefixedId.Prefix.MEMORY).uuid
        return feedbackService.listForMemory(userId, memoryId).map { it.toResponse(extId) }
    }

    private fun MemoryFeedback.toResponse(extId: String): FeedbackResponse =
        FeedbackResponse(
            memoryExtId = extId,
            target = target,
            value = value,
            note = note,
            createdAt = createdAt,
        )

    /** 요청 본문 — target/value(필수) + note(선택). */
    data class SubmitRequest(
        val target: String,
        val value: String,
        val note: String? = null,
    )

    /** 응답 — 본인 피드백 1건. */
    data class FeedbackResponse(
        val memoryExtId: String,
        val target: String,
        val value: String,
        val note: String?,
        val createdAt: OffsetDateTime,
    )
}
