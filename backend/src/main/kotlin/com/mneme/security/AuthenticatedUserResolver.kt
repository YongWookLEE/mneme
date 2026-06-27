package com.mneme.security

import com.mneme.auth.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 현재 인증된 사용자의 internal userId(UUID)를 추출하는 헬퍼.
 *
 * 두 가지 인증 경로를 통일된 인터페이스로 처리한다:
 * 1. `ApiKeyAuthenticationToken` — principal이 UUID 자체
 * 2. OAuth2 세션 → `OAuth2User` — sub로 users 테이블 lookup
 *
 * 인증되지 않은 호출은 401을 던진다.
 */
@Component
class AuthenticatedUserResolver(
    private val userRepository: UserRepository,
) {
    /** 현재 사용자 ID. 인증되지 않았으면 401. */
    fun currentUserId(): UUID {
        val auth: Authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val principal = auth.principal
        return when (principal) {
            is UUID -> principal
            is OAuth2User -> {
                val sub = principal.getAttribute<String>("sub") ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
                userRepository.findByGoogleSub(sub)?.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
            }
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
    }
}
