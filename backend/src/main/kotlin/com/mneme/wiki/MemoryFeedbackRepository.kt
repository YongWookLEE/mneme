package com.mneme.wiki

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 메모리 피드백 리포지토리.
 *
 * 격리: 모든 조회 메서드 첫 인자 userId.
 *
 * @author Mneme
 * @since phase 23
 */
@Repository
interface MemoryFeedbackRepository : JpaRepository<MemoryFeedback, UUID> {
    /** 본인 피드백 최신순. */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<MemoryFeedback>

    /** 특정 메모리에 남긴 본인 피드백 최신순. */
    fun findAllByUserIdAndMemoryIdOrderByCreatedAtDesc(
        userId: UUID,
        memoryId: UUID,
    ): List<MemoryFeedback>
}
