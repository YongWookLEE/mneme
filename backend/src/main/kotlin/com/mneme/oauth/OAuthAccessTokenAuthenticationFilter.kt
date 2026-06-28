package com.mneme.oauth

import com.mneme.auth.ApiKeyGenerator
import com.mneme.security.ApiKeyAuthenticationToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * OAuth access token Bearer 인증 필터.
 *
 * `Authorization: Bearer <opaque>`이지만 `mn_` prefix가 아닌(즉 ApiKey가 아닌) Bearer 토큰을 처리한다.
 * sha256 해시로 `oauth_tokens` 조회 후 SecurityContext에 `ApiKeyAuthenticationToken`(principal=userId)을 주입한다.
 * MCP 도구 인증은 [com.mneme.security.AuthenticatedUserResolver]가 이 토큰을 그대로 읽어 userId로 변환한다.
 *
 * ApiKeyAuthenticationFilter 뒤(또는 앞)에 등록한다. 본 필터는 ApiKey가 이미 인증을 설정했으면 건너뛴다.
 *
 * @author Mneme
 * @since phase 10
 */
class OAuthAccessTokenAuthenticationFilter(
    private val oauthService: OAuthService,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            val header = request.getHeader("Authorization")
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                val token = header.substring(BEARER_PREFIX.length).trim()
                // mn_ prefix는 ApiKeyAuthenticationFilter에서 이미 처리된다(또는 처리 실패 = 미인증).
                if (token.isNotEmpty() && !token.startsWith(ApiKeyGenerator.PLAIN_PREFIX)) {
                    val userId: UUID? = oauthService.authenticateAccessToken(token)
                    if (userId != null) {
                        SecurityContextHolder.getContext().authentication = ApiKeyAuthenticationToken(userId = userId, keyId = userId)
                        log.debug("OAuth access token authenticated: userId={}", userId)
                    }
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
