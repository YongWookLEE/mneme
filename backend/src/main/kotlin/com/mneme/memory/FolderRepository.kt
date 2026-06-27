package com.mneme.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 폴더 리포지토리.
 *
 * 모든 조회 메서드의 첫 인자는 `userId: UUID`. 격리 회귀 테스트는 phase 05/08에서 도입.
 */
@Repository
interface FolderRepository : JpaRepository<Folder, UUID> {
    /**
     * 사용자 본인의 단건 폴더.
     *
     * @param userId 사용자 ID
     * @param id 폴더 ID
     * @return 폴더 또는 null
     */
    fun findByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Folder?

    /**
     * 사용자 본인의 폴더 중 정규화 경로 일치.
     *
     * @param userId 사용자 ID
     * @param path materialized path (예: `/projects/mneme/`)
     * @return 폴더 또는 null
     */
    fun findByUserIdAndPath(
        userId: UUID,
        path: String,
    ): Folder?

    /**
     * 사용자 본인의 폴더 중 부모 직속 자식 목록.
     *
     * @param userId 사용자 ID
     * @param parentId 부모 폴더 ID (root는 null)
     * @return 자식 폴더 목록
     */
    fun findAllByUserIdAndParentId(
        userId: UUID,
        parentId: UUID?,
    ): List<Folder>

    /**
     * 사용자 본인의 모든 폴더.
     */
    fun findAllByUserId(userId: UUID): List<Folder>
}
