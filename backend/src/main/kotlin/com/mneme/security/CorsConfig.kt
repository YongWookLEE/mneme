package com.mneme.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * CORS 설정.
 *
 * `MNEME_FRONTEND_ORIGIN`에 등록된 정확한 origin만 허용한다. 와일드카드 금지. 운영에서는 본 값이
 * `https://mneme.example.com` 같은 단일 도메인이 된다.
 */
@Configuration
class CorsConfig {
    @Bean
    fun corsConfigurationSource(
        @Value("\${mneme.frontend-origin}") frontendOrigin: String,
    ): UrlBasedCorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOrigin(frontendOrigin)
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
