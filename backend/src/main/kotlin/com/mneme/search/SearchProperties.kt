package com.mneme.search

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 하이브리드 검색 가중치. 합이 1이 되도록 설정 권장(엄격 검사는 안 함 — 사용자가 강조를 바꿀 수 있게).
 *
 * - vectorAlpha: 임베딩 cosine 유사도(0~1) 가중치
 * - textBeta: tsvector ts_rank 가중치
 * - trigramGamma: pg_trgm similarity 가중치(오타·부분일치)
 */
@ConfigurationProperties(prefix = "mneme.search")
data class SearchProperties(
    val vectorAlpha: Double = 0.6,
    val textBeta: Double = 0.3,
    val trigramGamma: Double = 0.1,
    val defaultLimit: Int = 20,
    val maxLimit: Int = 100,
)
