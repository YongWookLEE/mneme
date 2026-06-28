package com.mneme.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * rate limit 강제 필터. 인증 필터 뒤에 등록.
 *
 * 인증된 사용자 → userId 기반 버킷. 미인증 → 클라이언트 IP fallback.
 * 쓰기 메서드(POST/PATCH/PUT/DELETE)는 별도 쓰기 버킷도 차감.
 */
@Component
@Order(50)
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val mapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (skip(request)) {
            filterChain.doFilter(request, response)
            return
        }
        val auth = SecurityContextHolder.getContext().authentication
        val isAuthenticated = auth?.isAuthenticated == true && auth.principal != "anonymousUser"
        val principalKey =
            when {
                isAuthenticated -> "u:${auth.name ?: auth.principal}"
                else -> "ip:${clientIp(request)}"
            }
        val isWrite = request.method in WRITE_METHODS
        if (!rateLimitService.acquire(principalKey, isWrite, isAuthenticated)) {
            writeRateLimited(response)
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun skip(req: HttpServletRequest): Boolean {
        val p = req.requestURI
        return p.startsWith("/actuator/health") || p.startsWith("/login") || p.startsWith("/oauth")
    }

    private fun clientIp(req: HttpServletRequest): String {
        val xff =
            req
                .getHeader("X-Forwarded-For")
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
        return if (!xff.isNullOrBlank()) xff else req.remoteAddr ?: "unknown"
    }

    private fun writeRateLimited(response: HttpServletResponse) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        val body =
            mapOf(
                "type" to "urn:mneme:error:ERR_RATE_LIMIT",
                "title" to "error.rate-limit",
                "status" to 429,
                "code" to "ERR_RATE_LIMIT",
            )
        response.writer.write(mapper.writeValueAsString(body))
    }

    companion object {
        private val WRITE_METHODS = setOf("POST", "PATCH", "PUT", "DELETE")
    }
}
