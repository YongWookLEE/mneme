package com.mneme.oauth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * OAuth access/refresh token.
 *
 * `accessHash`와 `refreshHash`는 sha256 결과. 평문은 토큰 발급 응답에서만 1회 노출.
 * `scope`는 phase 10에서는 `mcp` 단일. `revokedAt` 비-null이면 검증 시 401.
 *
 * @author Mneme
 * @since phase 10
 */
@Entity
@Table(name = "oauth_tokens")
class OAuthToken(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "client_id", nullable = false, updatable = false)
    val clientId: String,
    @Column(name = "access_hash", nullable = false)
    var accessHash: ByteArray,
    @Column(name = "refresh_hash")
    var refreshHash: ByteArray? = null,
    @Column(name = "scope")
    var scope: String? = "mcp",
    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,
    @Column(name = "refresh_expires_at")
    var refreshExpiresAt: OffsetDateTime? = null,
    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
