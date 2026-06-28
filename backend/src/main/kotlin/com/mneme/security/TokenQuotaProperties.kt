package com.mneme.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 사용자별 일일 외부 LLM 토큰 한도. 초과 시 임베딩/채팅 호출 차단.
 */
@ConfigurationProperties(prefix = "mneme.token-quota")
data class TokenQuotaProperties(
    val embedPerDay: Int = 100_000,
    val llmPerDay: Int = 50_000,
)
