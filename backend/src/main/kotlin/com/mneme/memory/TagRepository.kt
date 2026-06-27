package com.mneme.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 태그 리포지토리.
 *
 * 태그 이름은 사용자별 unique. 이름 매칭은 소문자 기준(서비스에서 정규화).
 */
@Repository
interface TagRepository : JpaRepository<Tag, UUID> {
    /**
     * 사용자 본인의 태그 단건(이름).
     */
    fun findByUserIdAndName(
        userId: UUID,
        name: String,
    ): Tag?

    /**
     * 사용자 본인의 모든 태그.
     */
    fun findAllByUserId(userId: UUID): List<Tag>
}
