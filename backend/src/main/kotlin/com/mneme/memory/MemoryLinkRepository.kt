package com.mneme.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 메모리 링크 리포지토리.
 *
 * 본문 `[[wiki-link]]` 파싱 결과의 파생 인덱스. 본문 저장 시 트랜잭션 안에서 `deleteAllByUserIdAndSourceId` 후
 * 재삽입하는 것이 기본 흐름(diff 갱신은 후속 최적화).
 */
@Repository
interface MemoryLinkRepository : JpaRepository<MemoryLink, UUID> {
    /**
     * 사용자 본인의 메모리에서 출발하는 링크(forward).
     */
    fun findAllByUserIdAndSourceId(
        userId: UUID,
        sourceId: UUID,
    ): List<MemoryLink>

    /**
     * 사용자 본인의 메모리로 들어오는 링크(backlink).
     */
    fun findAllByUserIdAndTargetId(
        userId: UUID,
        targetId: UUID,
    ): List<MemoryLink>

    /**
     * 사용자 본인의 깨진 링크(target_id IS NULL).
     */
    fun findAllByUserIdAndTargetIdIsNull(userId: UUID): List<MemoryLink>

    /**
     * 특정 소스의 모든 forward 링크를 삭제(본문 재파싱 전).
     */
    fun deleteAllByUserIdAndSourceId(
        userId: UUID,
        sourceId: UUID,
    )
}
