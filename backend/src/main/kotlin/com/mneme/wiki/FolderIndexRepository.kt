package com.mneme.wiki

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 폴더 인덱스 리포지토리.
 *
 * 격리: 모든 조회 메서드의 첫 인자는 `userId`.
 *
 * @author Mneme
 * @since phase 21
 */
@Repository
interface FolderIndexRepository : JpaRepository<FolderIndex, UUID> {
    /** 본인 폴더의 인덱스 단건 조회. */
    fun findByUserIdAndFolderId(
        userId: UUID,
        folderId: UUID,
    ): FolderIndex?
}
