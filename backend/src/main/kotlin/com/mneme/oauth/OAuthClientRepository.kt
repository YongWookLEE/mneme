package com.mneme.oauth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * OAuth 클라이언트 리포지토리. clientId 기준 조회가 일반적.
 *
 * @author Mneme
 * @since phase 10
 */
@Repository
interface OAuthClientRepository : JpaRepository<OAuthClient, UUID> {
    /** clientId로 단건 조회. DCR 등록은 anonymous이므로 user_id 격리는 token 단계에서만 강제. */
    fun findByClientId(clientId: String): OAuthClient?
}
