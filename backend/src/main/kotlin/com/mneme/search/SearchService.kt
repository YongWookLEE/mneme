package com.mneme.search

import com.mneme.llm.EmbeddingService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 하이브리드 검색. α·vector cosine + β·tsv ts_rank + γ·trgm similarity.
 *
 * 사용자 격리는 `WHERE user_id = :uid`로 강제. archived 제외 기본. 폴더/태그/날짜 필터 옵션.
 * 단일 SQL — 결과 페이지네이션은 LIMIT만(검색은 보통 상위 20).
 */
@Service
class SearchService(
    private val embeddingService: EmbeddingService,
    private val props: SearchProperties,
    @PersistenceContext private val em: EntityManager,
) {
    /** 검색 실행. 빈 query는 호출 측에서 사전 차단. */
    @Transactional(readOnly = true)
    fun search(
        userId: UUID,
        query: String,
        filter: SearchFilter = SearchFilter(),
        limit: Int = props.defaultLimit,
    ): List<SearchHit> {
        require(query.isNotBlank()) { "검색어가 비어 있습니다" }
        val effectiveLimit = limit.coerceIn(1, props.maxLimit)
        val qvec = embeddingService.embed(userId, query)
        val qvecLiteral = qvec.joinToString(prefix = "[", postfix = "]", separator = ",")

        val sql =
            """
            SELECT m.id, m.folder_id, m.title, m.summary, m.created_at, m.updated_at,
                   (:alpha * (1 - (m.embedding <=> CAST(:qvec AS vector))))
                   + (:beta  * ts_rank(m.tsv, plainto_tsquery('simple', :q)))
                   + (:gamma * similarity(m.content, :q)) AS score
            FROM memories m
            WHERE m.user_id = :uid
              AND m.archived_at IS NULL
              AND m.embedding IS NOT NULL
              AND (CAST(:folderId AS uuid) IS NULL OR m.folder_id = CAST(:folderId AS uuid))
              AND (CAST(:fromTs AS timestamptz) IS NULL OR m.created_at >= CAST(:fromTs AS timestamptz))
              AND (CAST(:toTs AS timestamptz) IS NULL OR m.created_at <= CAST(:toTs AS timestamptz))
              AND (:tagCount = 0 OR EXISTS (
                    SELECT 1 FROM memory_tags mt JOIN tags t ON t.id = mt.tag_id
                    WHERE mt.memory_id = m.id AND t.user_id = :uid AND t.name = ANY(CAST(:tagNames AS text[]))
              ))
            ORDER BY score DESC
            LIMIT :lim
            """.trimIndent()

        val tagArray = if (filter.tagNames.isEmpty()) "{}" else filter.tagNames.joinToString(prefix = "{", postfix = "}", separator = ",")

        @Suppress("UNCHECKED_CAST")
        val rows =
            em
                .createNativeQuery(sql)
                .setParameter("alpha", props.vectorAlpha)
                .setParameter("beta", props.textBeta)
                .setParameter("gamma", props.trigramGamma)
                .setParameter("qvec", qvecLiteral)
                .setParameter("q", query)
                .setParameter("uid", userId)
                .setParameter("folderId", filter.folderId?.toString())
                .setParameter("fromTs", filter.from?.toString())
                .setParameter("toTs", filter.to?.toString())
                .setParameter("tagCount", filter.tagNames.size)
                .setParameter("tagNames", tagArray)
                .setParameter("lim", effectiveLimit)
                .resultList as List<Array<Any?>>

        return rows.map { row ->
            SearchHit(
                id = row[0] as UUID,
                folderId = row[1] as UUID,
                title = row[2] as String,
                summary = row[3] as String?,
                createdAt = toOffsetDateTime(row[4]),
                updatedAt = toOffsetDateTime(row[5]),
                score = (row[6] as Number).toDouble(),
            )
        }
    }

    private fun toOffsetDateTime(value: Any?): OffsetDateTime =
        when (value) {
            is OffsetDateTime -> value
            is Instant -> value.atOffset(ZoneOffset.UTC)
            is Timestamp -> value.toInstant().atOffset(ZoneOffset.UTC)
            else -> error("지원하지 않는 timestamp 타입: ${value?.javaClass}")
        }
}

/** 검색 필터 옵션. */
data class SearchFilter(
    val folderId: UUID? = null,
    val tagNames: List<String> = emptyList(),
    val from: OffsetDateTime? = null,
    val to: OffsetDateTime? = null,
)

/** 검색 결과 항목. 본문은 응답에서 제외(상세는 별도 GET). */
data class SearchHit(
    val id: UUID,
    val folderId: UUID,
    val title: String,
    val summary: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val score: Double,
)
