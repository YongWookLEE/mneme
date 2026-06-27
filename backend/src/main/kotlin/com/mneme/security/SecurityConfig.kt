package com.mneme.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security 기본 설정.
 *
 * 공개 경로: actuator health probes, OAuth 콜백, login 시작. 나머지는 인증 필요.
 * OAuth2 provider 등록 / CSRF / 보안 헤더 / CORS는 step 2~3에서 추가.
 */
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
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
        return http.build()
    }
}
