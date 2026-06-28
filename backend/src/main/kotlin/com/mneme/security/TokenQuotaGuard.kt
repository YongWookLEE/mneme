package com.mneme.security

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * 사용자별 일일 LLM 토큰 한도 강제. `usage_daily` 조회 → 예상 토큰 추가 시 한도 초과면 차단.
 *
 * 차단은 [QuotaExceededException]을 던져 호출자가 `OpenAiException.RateLimited`로 변환하거나
 * swallow한다. 본 가드는 외부 호출 직전에 호출.
 */
@Service
class TokenQuotaGuard(
    private val props: TokenQuotaProperties,
    private val clock: Clock = Clock.systemUTC(),
    @PersistenceContext private val em: EntityManager,
) {
    @Transactional(readOnly = true)
    fun requireEmbedBudget(
        userId: UUID,
        estimatedTokens: Int,
    ) {
        val used = readEmbedUsed(userId, today())
        if (used + estimatedTokens > props.embedPerDay) {
            throw QuotaExceededException("embed daily quota: $used + $estimatedTokens > ${props.embedPerDay}")
        }
    }

    @Transactional(readOnly = true)
    fun requireChatBudget(
        userId: UUID,
        estimatedTokens: Int,
    ) {
        val used = readChatUsed(userId, today())
        if (used + estimatedTokens > props.llmPerDay) {
            throw QuotaExceededException("llm daily quota: $used + $estimatedTokens > ${props.llmPerDay}")
        }
    }

    private fun today(): LocalDate = clock.instant().atZone(ZoneOffset.UTC).toLocalDate()

    private fun readEmbedUsed(
        userId: UUID,
        date: LocalDate,
    ): Int =
        (
            em
                .createNativeQuery("SELECT COALESCE(embed_tokens, 0) FROM usage_daily WHERE user_id = :uid AND date = :d")
                .setParameter("uid", userId)
                .setParameter("d", date)
                .resultList
                .firstOrNull() as? Number
        )?.toInt() ?: 0

    private fun readChatUsed(
        userId: UUID,
        date: LocalDate,
    ): Int =
        (
            em
                .createNativeQuery(
                    "SELECT COALESCE(llm_in_tokens, 0) + COALESCE(llm_out_tokens, 0) FROM usage_daily WHERE user_id = :uid AND date = :d",
                ).setParameter("uid", userId)
                .setParameter("d", date)
                .resultList
                .firstOrNull() as? Number
        )?.toInt() ?: 0
}

class QuotaExceededException(
    message: String,
) : RuntimeException(message)
