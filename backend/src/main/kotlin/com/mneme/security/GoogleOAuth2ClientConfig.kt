package com.mneme.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

/**
 * Google OAuth2 클라이언트 등록.
 *
 * `mneme.google-oauth.client-id`가 비어 있지 않을 때만 빈을 생성한다. 이로써 로컬 개발이나 CI에서 키 없이도
 * 앱이 정상 부팅된다(Spring Boot의 기본 `OAuth2ClientAutoConfiguration`은 빈 client-id에 대해 실패하므로
 * 우리 쪽 application.yml에서 `spring.security.oauth2.client.registration.*` 키를 제공하지 않고 본 빈으로 대체).
 */
@Configuration
@ConditionalOnExpression("'\${mneme.google-oauth.client-id:}' != ''")
class GoogleOAuth2ClientConfig {
    @Bean
    fun clientRegistrationRepository(
        @Value("\${mneme.google-oauth.client-id}") clientId: String,
        @Value("\${mneme.google-oauth.client-secret}") clientSecret: String,
        @Value("\${mneme.base-url}") baseUrl: String,
    ): ClientRegistrationRepository {
        val google: ClientRegistration =
            CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "email", "profile")
                .redirectUri("$baseUrl/login/oauth2/code/google")
                .build()
        return InMemoryClientRegistrationRepository(google)
    }
}
