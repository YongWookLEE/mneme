package com.mneme.wiki

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 메모리 분류·요약 결과에 대한 사용자 피드백.
 *
 * `target`은 평가 대상(`folder`, `summary`, `tags`, `index`, `general`).
 * `value`는 `up`/`down`. `note`는 선택적 자유 메모.
 *
 * @author Mneme
 * @since phase 23
 */
@Entity
@Table(name = "memory_feedback")
class MemoryFeedback(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "memory_id", nullable = false, updatable = false)
    val memoryId: UUID,
    @Column(name = "target", nullable = false)
    val target: String,
    @Column(name = "value", nullable = false)
    val value: String,
    @Column(name = "note")
    val note: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
