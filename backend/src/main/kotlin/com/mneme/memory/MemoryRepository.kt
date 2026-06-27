package com.mneme.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 메모리 리포지토리.
 *
 * 모든 조회 메서드의 첫 인자는 `userId: UUID`. CRITICAL: `findById(id)`는 직접 사용 금지 — 격리 우회.
 * 폐기된 메모리는 `archived_at IS NOT NULL`. 기본 조회는 archived 제외.
 */
@Repository
interface MemoryRepository : JpaRepository<Memory, UUID> {
    /**
     * 사용자 본인의 단건 메모리(archived 포함).
     *
     * @param userId 사용자 ID
     * @param id 메모리 ID
     * @return 메모리 또는 null
     */
    fun findByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Memory?

    /**
     * 사용자 본인의 활성(archived 아님) 메모리 단건.
     */
    fun findByUserIdAndIdAndArchivedAtIsNull(
        userId: UUID,
        id: UUID,
    ): Memory?

    /**
     * 사용자 본인의 폴더 내 활성 메모리 목록.
     */
    fun findAllByUserIdAndFolderIdAndArchivedAtIsNull(
        userId: UUID,
        folderId: UUID,
    ): List<Memory>

    /**
     * 사용자 본인의 활성 메모리 전체.
     */
    fun findAllByUserIdAndArchivedAtIsNull(userId: UUID): List<Memory>

    /**
     * 사용자 본인의 archived 메모리 목록(/archive 페이지용).
     */
    fun findAllByUserIdAndArchivedAtIsNotNull(userId: UUID): List<Memory>

    /**
     * 사용자 본인 + 정확한 제목으로 단건 조회. wiki-link 파싱 시 제목 → ID 매칭용.
     */
    fun findByUserIdAndTitleAndArchivedAtIsNull(
        userId: UUID,
        title: String,
    ): Memory?
}
