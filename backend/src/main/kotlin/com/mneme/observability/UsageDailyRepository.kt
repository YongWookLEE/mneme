package com.mneme.observability

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.time.LocalDate
import java.util.UUID

/**
 * `usage_daily` 네이티브 조회 리포지토리.
 *
 * 별도 엔티티 매핑 없이 native query로 일별 토큰·요청 수를 가져온다(필드 4개 + 날짜).
 *
 * @author Mneme
 * @since phase 14
 */
@Repository
class UsageDailyRepository(
    @PersistenceContext private val em: EntityManager,
) {
    /** 지정 사용자/구간(포함)에서 일별 사용량 행. */
    @Transactional(readOnly = true)
    fun findRange(
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<UsageDailyRow> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            em
                .createNativeQuery(
                    """
                    SELECT date, embed_tokens, llm_in_tokens, llm_out_tokens, request_count
                    FROM usage_daily
                    WHERE user_id = :uid AND date BETWEEN :from AND :to
                    ORDER BY date DESC
                    """.trimIndent(),
                ).setParameter("uid", userId)
                .setParameter("from", Date.valueOf(from))
                .setParameter("to", Date.valueOf(to))
                .resultList as List<Array<Any?>>
        return rows.map { r ->
            UsageDailyRow(
                date =
                    when (val v = r[0]) {
                        is Date -> v.toLocalDate()
                        is LocalDate -> v
                        else -> error("unexpected date type: ${v?.javaClass}")
                    },
                embedTokens = (r[1] as Number).toInt(),
                llmInTokens = (r[2] as Number).toInt(),
                llmOutTokens = (r[3] as Number).toInt(),
                requestCount = (r[4] as Number).toInt(),
            )
        }
    }
}

/** usage_daily 1행. */
data class UsageDailyRow(
    val date: LocalDate,
    val embedTokens: Int,
    val llmInTokens: Int,
    val llmOutTokens: Int,
    val requestCount: Int,
)
