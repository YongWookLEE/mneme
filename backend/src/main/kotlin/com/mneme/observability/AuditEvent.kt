package com.mneme.observability

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 감사 이벤트 엔티티.
 *
 * action은 점으로 구분된 도메인 식별자(예: `key.created`, `key.revoked`, `memory.archived`).
 * actor_kind는 `user`/`api_key`/`oauth` 중 하나. metadata는 JSONB이지만 본 phase에서는 String으로만 다루고
 * 본격 JSONB 매핑은 phase 14(observability)에서 도입.
 */
@Entity
@Table(name = "audit_events")
class AuditEvent(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id")
    val userId: UUID? = null,
    @Column(name = "actor_kind", nullable = false)
    val actorKind: String,
    @Column(name = "action", nullable = false)
    val action: String,
    @Column(name = "target_kind")
    val targetKind: String? = null,
    @Column(name = "target_id")
    val targetId: String? = null,
    @Column(name = "ip", columnDefinition = "inet")
    val ip: String? = null,
    @Column(name = "user_agent")
    val userAgent: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
