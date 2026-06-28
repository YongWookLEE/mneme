package com.mneme.security

import com.mneme.auth.ApiKeyGenerator
import com.mneme.auth.ApiKeyRepository
import com.mneme.auth.OAuth2LoginSuccessHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Spring Security 기본 설정.
 *
 * 공개 경로: actuator health probes, OAuth 콜백, login 시작. 나머지는 인증 필요.
 * OAuth2 Login은 `ClientRegistrationRepository` 빈이 등록될 때만 활성화(환경변수 조건부).
 * CSRF / 보안 헤더 / CORS는 step 3에서 추가.
 */
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        corsConfigurationSource: CorsConfigurationSource,
        clientRegistrationRepository: ObjectProvider<ClientRegistrationRepository>,
        successHandler: ObjectProvider<OAuth2LoginSuccessHandler>,
        apiKeyRepository: ApiKeyRepository,
        apiKeyGenerator: ApiKeyGenerator,
    ): SecurityFilterChain {
        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/oauth/**",
                    "/login",
                    "/login/**",
                    "/error",
                ).permitAll()
            auth.anyRequest().authenticated()
        }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
        http.httpBasic { it.disable() }
        http.formLogin { it.disable() }
        http.cors(Customizer.withDefaults())
        // Bearer 토큰 인증(API 키·MCP OAuth)은 쿠키 ambient 인증이 아니므로 CSRF 불필요.
        val bearerMatcher =
            RequestMatcher { req ->
                req.getHeader("Authorization")?.startsWith("Bearer ") == true
            }
        http.csrf { csrf ->
            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            csrf.ignoringRequestMatchers(bearerMatcher)
        }
        http.headers { headers ->
            headers.contentSecurityPolicy {
                it.policyDirectives(
                    "default-src 'self'; img-src 'self' data: https:; " +
                        "style-src 'self' 'unsafe-inline'; script-src 'self'; " +
                        "connect-src 'self' https:; frame-ancestors 'none'",
                )
            }
            headers.frameOptions { it.deny() }
            headers.contentTypeOptions(Customizer.withDefaults())
            headers.referrerPolicy {
                it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            }
            headers.xssProtection { it.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) }
            headers.permissionsPolicy { it.policy("camera=(), microphone=(), geolocation=()") }
        }

        // API Key Bearer 인증 필터 (UsernamePasswordAuthenticationFilter 앞)
        http.addFilterBefore(
            ApiKeyAuthenticationFilter(apiKeyRepository, apiKeyGenerator),
            UsernamePasswordAuthenticationFilter::class.java,
        )

        // OAuth2 client registration이 환경 변수로 활성화돼 있을 때만 oauth2Login 체인 등록
        val repo = clientRegistrationRepository.ifAvailable
        if (repo != null) {
            http.oauth2Login { oauth ->
                successHandler.ifAvailable?.let { oauth.successHandler(it) }
            }
        }
        return http.build()
    }
}
