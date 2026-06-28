package com.mneme.api

import com.mneme.observability.AuditEventRepository
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * 본인 감사 이벤트 조회 — `GET /api/audit`.
 *
 * 사용자 본인의 이벤트만 최신순. 다른 사용자 이벤트는 절대 노출하지 않는다.
 *
 * @author Mneme
 * @since phase 14
 */
@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val auditRepository: AuditEventRepository,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 본인 이벤트 목록(최신순). */
    @GetMapping
    fun list(): List<AuditEventResponse> {
        val userId = userResolver.currentUserId()
        return auditRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { e ->
            AuditEventResponse(
                actorKind = e.actorKind,
                action = e.action,
                targetKind = e.targetKind,
                targetId = e.targetId,
                createdAt = e.createdAt,
            )
        }
    }

    /** 응답 DTO. user_id/ip/user_agent는 클라이언트에 노출하지 않는다. */
    data class AuditEventResponse(
        val actorKind: String,
        val action: String,
        val targetKind: String?,
        val targetId: String?,
        val createdAt: OffsetDateTime,
    )
}
