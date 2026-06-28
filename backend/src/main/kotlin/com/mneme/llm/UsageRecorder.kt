package com.mneme.llm

import java.time.LocalDate
import java.util.UUID

/**
 * 토큰 사용량 기록 인터페이스. 구현은 step 4에서 `usage_daily` 테이블에 upsert.
 *
 * 본 phase는 임베딩만 필수. 채팅 토큰 집계는 phase 08(보안 통제)에서 한도 강제와 함께 강화.
 */
interface UsageRecorder {
    fun recordEmbedTokens(
        userId: UUID,
        date: LocalDate,
        tokens: Int,
    )

    fun recordChatTokens(
        userId: UUID,
        date: LocalDate,
        inputTokens: Int,
        outputTokens: Int,
    )
}
