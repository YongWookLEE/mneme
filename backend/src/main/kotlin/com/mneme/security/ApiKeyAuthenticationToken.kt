package com.mneme.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * API 키로 인증된 사용자의 Authentication 토큰.
 *
 * `principal`은 userId(UUID). 권한은 단순히 `ROLE_USER`. MCP/REST 컨트롤러에서 `Authentication.principal as UUID`로 사용.
 */
class ApiKeyAuthenticationToken(
    val userId: UUID,
    val keyId: UUID,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_USER"))) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = ""

    override fun getPrincipal(): Any = userId
}
