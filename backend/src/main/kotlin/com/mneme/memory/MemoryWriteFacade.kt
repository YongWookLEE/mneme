package com.mneme.memory

import com.mneme.llm.ChatService
import com.mneme.llm.EmbeddingService
import com.mneme.llm.OpenAiException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 메모리 쓰기 외부 호출 결합. 컨트롤러는 본 facade를 사용한다.
 *
 * 흐름:
 * 1. 본문 임베딩 + 요약을 트랜잭션 밖에서 계산(OpenAI 호출은 트랜잭션 밖 — CLAUDE.md 규칙).
 * 2. [MemoryService.create] / [MemoryService.update]가 자체 트랜잭션으로 메모리 저장.
 * 3. [MemoryEmbeddingDao.updateEmbedding]가 별도 트랜잭션으로 임베딩 컬럼 갱신.
 *
 * 외부 호출 실패 시 메모리 저장은 막지 않는다(요약은 null, 임베딩은 skip). 임베딩은 phase 07
 * 백필 잡으로 재시도.
 */
@Component
class MemoryWriteFacade(
    private val memoryService: MemoryService,
    private val embeddingService: EmbeddingService,
    private val chatService: ChatService,
    private val embeddingDao: MemoryEmbeddingDao,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 메모리 생성 + 임베딩/요약. 컨트롤러 진입점. */
    fun create(
        userId: UUID,
        folderId: UUID,
        title: String,
        content: String,
        summary: String?,
        sourceUri: String?,
    ): Memory {
        val effectiveSummary = summary ?: tryGenerateSummary(userId, content)
        val embedding = tryComputeEmbedding(userId, content)
        val memory = memoryService.create(userId, folderId, title, content, effectiveSummary, sourceUri)
        if (embedding != null) embeddingDao.updateEmbedding(userId, memory.id, embedding)
        return memory
    }

    /** 메모리 갱신 + 본문 변경 시 임베딩 재계산. */
    fun update(
        userId: UUID,
        memoryId: UUID,
        expectedVersion: Long,
        title: String?,
        content: String?,
        summary: String?,
        folderId: UUID?,
    ): Memory {
        val embedding = content?.let { tryComputeEmbedding(userId, it) }
        val memory = memoryService.update(userId, memoryId, expectedVersion, title, content, summary, folderId)
        if (embedding != null) embeddingDao.updateEmbedding(userId, memory.id, embedding)
        return memory
    }

    private fun tryComputeEmbedding(
        userId: UUID,
        content: String,
    ): FloatArray? =
        try {
            embeddingService.embed(userId, content)
        } catch (e: OpenAiException) {
            log.warn("임베딩 계산 실패, 저장은 계속: {}", e.message)
            null
        }

    private fun tryGenerateSummary(
        userId: UUID,
        content: String,
    ): String? = chatService.summarize(userId, content)
}
