package com.mneme.llm

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OpenAI 어댑터 설정. `mneme.openai.*`로 바인딩되며 비밀값(api-key)은 빈 문자열로 두면 어댑터가
 * 호출 시 명시적으로 실패한다. ADR-020 참조.
 */
@ConfigurationProperties(prefix = "mneme.openai")
data class LlmProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com",
    val embedModel: String = "text-embedding-3-small",
    val chatModel: String = "gpt-4o-mini",
    val requestTimeoutSeconds: Long = 30,
    val embeddingCacheMaxSize: Long = 5_000,
    val embeddingCacheTtlMinutes: Long = 30,
)
