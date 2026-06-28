package com.mneme.observability

import com.mneme.llm.UsageRecorder
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/**
 * `usage_daily` 테이블 upsert 구현. (user_id, date) 복합 PK에 ON CONFLICT DO UPDATE.
 *
 * 메모리 저장 트랜잭션과 독립적으로 동작해야 하므로 `REQUIRES_NEW`. 사용량 기록 실패가 본문 저장을
 * 방해하지 않게 한다(상위에서 swallow 권장은 호출 측 책임).
 */
@Component("pgUsageRecorder")
@Primary
class PgUsageRecorder(
    @PersistenceContext private val em: EntityManager,
) : UsageRecorder {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun recordEmbedTokens(
        userId: UUID,
        date: LocalDate,
        tokens: Int,
    ) {
        em
            .createNativeQuery(
                """
                INSERT INTO usage_daily (user_id, date, embed_tokens, request_count)
                VALUES (:uid, :d, :t, 1)
                ON CONFLICT (user_id, date) DO UPDATE
                SET embed_tokens = usage_daily.embed_tokens + EXCLUDED.embed_tokens,
                    request_count = usage_daily.request_count + 1
                """.trimIndent(),
            ).setParameter("uid", userId)
            .setParameter("d", date)
            .setParameter("t", tokens)
            .executeUpdate()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun recordChatTokens(
        userId: UUID,
        date: LocalDate,
        inputTokens: Int,
        outputTokens: Int,
    ) {
        em
            .createNativeQuery(
                """
                INSERT INTO usage_daily (user_id, date, llm_in_tokens, llm_out_tokens, request_count)
                VALUES (:uid, :d, :i, :o, 1)
                ON CONFLICT (user_id, date) DO UPDATE
                SET llm_in_tokens = usage_daily.llm_in_tokens + EXCLUDED.llm_in_tokens,
                    llm_out_tokens = usage_daily.llm_out_tokens + EXCLUDED.llm_out_tokens,
                    request_count = usage_daily.request_count + 1
                """.trimIndent(),
            ).setParameter("uid", userId)
            .setParameter("d", date)
            .setParameter("i", inputTokens)
            .setParameter("o", outputTokens)
            .executeUpdate()
    }
}
