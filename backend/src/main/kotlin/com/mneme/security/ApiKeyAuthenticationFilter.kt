package com.mneme.security

import com.mneme.auth.ApiKeyGenerator
import com.mneme.auth.ApiKeyRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.OffsetDateTime

/**
 * API 키 Bearer 인증 필터.
 *
 * `Authorization: Bearer mn_<plaintext>` 헤더가 있으면:
 * 1. 앞 8자(prefix) 추출 → ApiKeyRepository.findAllByPrefixAndRevokedAtIsNull 후보 조회
 * 2. 후보의 keyHash와 SHA-256 비교(constant-time)
 * 3. 일치하면 SecurityContext에 ApiKeyAuthenticationToken 주입 + `lastUsedAt` 갱신
 *
 * 헤더가 없거나 Bearer가 아니거나 일치하는 키가 없으면 SecurityContext를 건드리지 않는다(익명).
 * 보호 엔드포인트는 후속 인증 처리(OAuth2 또는 401).
 */
class ApiKeyAuthenticationFilter(
    private val apiKeyRepository: ApiKeyRepository,
    private val apiKeyGenerator: ApiKeyGenerator,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val plaintext = header.substring(BEARER_PREFIX.length).trim()
            if (plaintext.startsWith(ApiKeyGenerator.PLAIN_PREFIX) && plaintext.length > ApiKeyGenerator.IDENTIFIER_LEN) {
                authenticate(plaintext)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(plaintext: String) {
        val prefix = plaintext.take(ApiKeyGenerator.IDENTIFIER_LEN)
        val candidates = apiKeyRepository.findAllByPrefixAndRevokedAtIsNull(prefix)
        for (key in candidates) {
            if (apiKeyGenerator.verify(plaintext, key.keyHash)) {
                val auth = ApiKeyAuthenticationToken(userId = key.userId, keyId = key.id)
                SecurityContextHolder.getContext().authentication = auth
                key.lastUsedAt = OffsetDateTime.now()
                log.debug("API key authenticated: userId={}, keyId={}", key.userId, key.id)
                return
            }
        }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
