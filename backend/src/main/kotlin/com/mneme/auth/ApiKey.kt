package com.mneme.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * API 키 엔티티.
 *
 * 평문 키는 발급 직후 1회만 응답에 노출되고 DB에는 sha256 해시(`keyHash`)와 앞 8자(`prefix`)만 저장한다.
 * 키 검증은 prefix로 빠르게 인덱스 조회 → 후보의 keyHash와 일치 비교. `revokedAt`이 설정되면 인덱스에서 제외(`prefix_idx` partial).
 */
@Entity
@Table(name = "api_keys")
class ApiKey(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "key_hash", nullable = false, updatable = false)
    val keyHash: ByteArray,
    @Column(name = "prefix", nullable = false, updatable = false)
    val prefix: String,
    @Column(name = "last_used_at")
    var lastUsedAt: OffsetDateTime? = null,
    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
