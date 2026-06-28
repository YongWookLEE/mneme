package com.mneme.oauth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * OAuth 토큰 리포지토리.
 *
 * access/refresh 해시는 sha256(bytea)로 인덱스 조회. 격리는 호출자가 user_id로 cross-check.
 *
 * @author Mneme
 * @since phase 10
 */
@Repository
interface OAuthTokenRepository : JpaRepository<OAuthToken, UUID> {
    /** access_hash 부분 인덱스(WHERE revoked_at IS NULL)로 단건 조회. */
    fun findByAccessHashAndRevokedAtIsNull(accessHash: ByteArray): OAuthToken?

    /** refresh 회전용. */
    fun findByRefreshHashAndRevokedAtIsNull(refreshHash: ByteArray): OAuthToken?
}
