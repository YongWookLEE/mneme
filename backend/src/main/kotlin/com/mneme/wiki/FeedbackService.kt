package com.mneme.wiki

import com.mneme.id.IdFactory
import com.mneme.memory.MemoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 메모리 피드백 도메인 서비스.
 *
 * 피드백 저장 + 본인 피드백 조회 + LLM 시스템 프롬프트에 들어갈 "최근 피드백 요약" 빌더(phase 06 ChatService가
 * 이 결과를 받아 system prompt 후미에 붙이도록 후속 확장 가능).
 *
 * 격리: 모든 메서드 첫 인자 userId.
 *
 * @author Mneme
 * @since phase 23
 */
@Service
@Transactional
class FeedbackService(
    private val repository: MemoryFeedbackRepository,
    private val memoryRepository: MemoryRepository,
    private val idFactory: IdFactory,
) {
    /** 피드백 1건 기록. */
    fun submit(
        userId: UUID,
        memoryId: UUID,
        target: String,
        value: String,
        note: String?,
    ): MemoryFeedback {
        require(target in ALLOWED_TARGETS) { "unknown_target" }
        require(value in ALLOWED_VALUES) { "invalid_value" }
        memoryRepository.findByUserIdAndId(userId, memoryId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val entity =
            MemoryFeedback(
                id = idFactory.newUuid(),
                userId = userId,
                memoryId = memoryId,
                target = target,
                value = value,
                note = note?.take(MAX_NOTE_LEN),
            )
        repository.save(entity)
        return entity
    }

    /** 특정 메모리에 남긴 본인 피드백. */
    @Transactional(readOnly = true)
    fun listForMemory(
        userId: UUID,
        memoryId: UUID,
    ): List<MemoryFeedback> {
        memoryRepository.findByUserIdAndId(userId, memoryId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return repository.findAllByUserIdAndMemoryIdOrderByCreatedAtDesc(userId, memoryId)
    }

    /** 본인 전체 피드백. */
    @Transactional(readOnly = true)
    fun listAll(userId: UUID): List<MemoryFeedback> = repository.findAllByUserIdOrderByCreatedAtDesc(userId)

    companion object {
        val ALLOWED_TARGETS = setOf("folder", "summary", "tags", "index", "general")
        val ALLOWED_VALUES = setOf("up", "down")
        const val MAX_NOTE_LEN = 500
    }
}
