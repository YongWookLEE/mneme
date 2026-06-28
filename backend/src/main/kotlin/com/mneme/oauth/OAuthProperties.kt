package com.mneme.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OAuth/MCP 토큰 발급 설정.
 *
 * @property accessTtlMinutes access token TTL (기본 30분, ADR-007)
 * @property refreshTtlDays refresh token TTL (기본 30일, ADR-007)
 * @property codeTtlSeconds authorization code TTL (기본 600초/10분)
 *
 * @author Mneme
 * @since phase 10
 */
@ConfigurationProperties(prefix = "mneme.oauth")
data class OAuthProperties(
    val accessTtlMinutes: Long = 30,
    val refreshTtlDays: Long = 30,
    val codeTtlSeconds: Long = 600,
)
