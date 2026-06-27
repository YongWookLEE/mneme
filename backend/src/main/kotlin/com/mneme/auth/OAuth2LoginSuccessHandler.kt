package com.mneme.auth

import com.mneme.id.IdFactory
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Google OAuth2 로그인 성공 핸들러.
 *
 * Google이 발급한 `sub`(googleSub)로 사용자를 upsert한다. 처음 보는 sub면 신규 User INSERT,
 * 기존 sub면 email/locale만 동기화. 세션은 Spring Security가 이미 생성하므로 별도 작업 불필요.
 *
 * 로그인 후 리다이렉트 경로는 기본 `/`. 추후 onboarding 진입(첫 로그인이면 `/onboarding`)은 phase 12에서.
 */
@Component
class OAuth2LoginSuccessHandler(
    private val userRepository: UserRepository,
    private val idFactory: IdFactory,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)

    init {
        defaultTargetUrl = "/"
        setAlwaysUseDefaultTargetUrl(false)
    }

    /**
     * OAuth2 인증 성공 시 호출. principal에서 sub/email/locale을 읽어 users 테이블에 upsert.
     */
    @Transactional
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal
        if (principal is OAuth2User) {
            val sub =
                principal.getAttribute<String>("sub")
                    ?: error("Google OAuth2 principal에 'sub' 클레임이 없습니다")
            val email = principal.getAttribute<String>("email") ?: ""
            val locale = principal.getAttribute<String>("locale") ?: "ko"

            val existing = userRepository.findByGoogleSub(sub)
            if (existing == null) {
                val newUser =
                    User(
                        id = idFactory.newUuid(),
                        googleSub = sub,
                        email = email,
                        locale = locale,
                    )
                userRepository.save(newUser)
                log.info("신규 사용자 가입: googleSub={}", sub)
            } else {
                if (existing.email != email && email.isNotBlank()) {
                    existing.email = email
                }
                if (existing.locale != locale && locale.isNotBlank()) {
                    existing.locale = locale
                }
                log.debug("기존 사용자 로그인: googleSub={}", sub)
            }
        }
        super.onAuthenticationSuccess(request, response, authentication)
    }
}
