package com.mneme.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * rate limit 설정. 분/일 일반 + 분 쓰기 분리.
 */
@ConfigurationProperties(prefix = "mneme.rate-limit")
data class RateLimitProperties(
    val perMin: Int = 60,
    val perDay: Int = 5_000,
    val writePerMin: Int = 20,
    val anonymousPerMin: Int = 30,
)
