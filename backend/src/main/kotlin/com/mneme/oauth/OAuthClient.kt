package com.mneme.oauth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

/**
 * OAuth 동적 등록 클라이언트(RFC 7591).
 *
 * `userId`는 등록 시점에 NULL — 실제 사용자 귀속은 첫 authorize 동의 시점의 user_id가 token에 기록된다.
 * `clientSecretHash`는 sha256(secret). 평문은 등록 응답에서 1회만 노출.
 *
 * @author Mneme
 * @since phase 10
 */
@Entity
@Table(name = "oauth_clients")
class OAuthClient(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id")
    var userId: UUID? = null,
    @Column(name = "client_id", nullable = false, unique = true, updatable = false)
    val clientId: String,
    @Column(name = "client_secret_hash")
    var clientSecretHash: ByteArray? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "redirect_uris", nullable = false, columnDefinition = "text[]")
    var redirectUris: Array<String>,
    @Column(name = "client_name")
    var clientName: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
