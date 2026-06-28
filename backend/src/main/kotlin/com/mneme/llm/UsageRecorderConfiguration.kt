package com.mneme.llm

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.util.UUID

/**
 * 사용량 기록기 기본 설정. step 4에서 `usage_daily` 테이블에 INSERT … ON CONFLICT 하는 실제
 * 구현이 등록되면 본 no-op 빈은 자동 비활성화된다.
 */
@Configuration
class UsageRecorderConfiguration {
    @Bean
    @ConditionalOnMissingBean(UsageRecorder::class)
    fun noOpUsageRecorder(): UsageRecorder =
        object : UsageRecorder {
            override fun recordEmbedTokens(
                userId: UUID,
                date: LocalDate,
                tokens: Int,
            ) {
                // no-op
            }

            override fun recordChatTokens(
                userId: UUID,
                date: LocalDate,
                inputTokens: Int,
                outputTokens: Int,
            ) {
                // no-op
            }
        }
}
