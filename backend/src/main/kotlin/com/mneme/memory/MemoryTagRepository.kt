package com.mneme.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 메모리 ↔ 태그 조인 리포지토리.
 *
 * `MemoryTag`는 자체에 userId가 없으므로(메모리·태그 양쪽에 이미 있음) 격리는 호출자가 미리 `MemoryRepository` 또는
 * `TagRepository`로 userId 검증을 끝낸 ID로만 호출해야 한다. 본 리포지토리는 ID 결합만 책임진다.
 */
@Repository
interface MemoryTagRepository : JpaRepository<MemoryTag, MemoryTag.MemoryTagId> {
    /**
     * 메모리에 부착된 태그 ID 목록 조회용.
     */
    fun findAllByIdMemoryId(memoryId: UUID): List<MemoryTag>

    /**
     * 메모리에 부착된 모든 태그 관계 삭제(메모리 본문 갱신 또는 archive 시).
     */
    fun deleteAllByIdMemoryId(memoryId: UUID)
}
