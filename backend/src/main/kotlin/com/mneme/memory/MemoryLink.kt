package com.mneme.memory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 메모리 링크(본문 `[[wiki-link]]` 파싱 결과의 파생 인덱스).
 *
 * ADR-018에 따라 본문이 관계의 진실이고 이 테이블은 인덱스. 메모리 저장 시 트랜잭션 안에서 동기 갱신:
 * 본문에서 사라진 link는 삭제, 새 link는 insert. `targetId=NULL`이면 깨진 링크(제목 매칭 실패).
 *
 * `kind`는 현재 `wiki`만 사용. `auto`(임베딩 유사도 기반 추천)는 후속 phase에 예약.
 */
@Entity
@Table(name = "memory_links")
class MemoryLink(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "source_id", nullable = false)
    val sourceId: UUID,
    @Column(name = "target_id")
    var targetId: UUID? = null,
    @Column(name = "target_label", nullable = false)
    var targetLabel: String,
    @Column(name = "kind", nullable = false)
    var kind: String = "wiki",
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
