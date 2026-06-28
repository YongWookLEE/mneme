package com.mneme.memory

import com.mneme.llm.ChatService
import com.mneme.llm.EmbeddingService
import com.mneme.llm.OpenAiException
import com.mneme.wiki.BacklinkRenameService
import com.mneme.wiki.WikiLinkIndexer
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
 * 4. [WikiLinkIndexer.reindex]가 별도 트랜잭션으로 본문 `[[link]]` 인덱스 갱신.
 * 5. 제목 변경 시 [BacklinkRenameService.rename]이 다른 메모리 본문 안의 `[[옛 제목]]`을
 *    모두 새 제목으로 치환(별도 빈이라 AOP 프록시가 정상 동작).
 *
 * 외부 호출 실패 시 메모리 저장은 막지 않는다(요약은 null, 임베딩은 skip).
 */
@Component
class MemoryWriteFacade(
    private val memoryService: MemoryService,
    private val embeddingService: EmbeddingService,
    private val chatService: ChatService,
    private val embeddingDao: MemoryEmbeddingDao,
    private val wikiLinkIndexer: WikiLinkIndexer,
    private val backlinkRenameService: BacklinkRenameService,
    private val memoryRepository: MemoryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 메모리 생성 + 임베딩/요약 + 본문 wiki 인덱스. */
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
        wikiLinkIndexer.reindex(userId, memory.id, content)
        return memory
    }

    /** 메모리 갱신 + 본문 변경 시 임베딩 재계산 + 본문/제목 변경 시 wiki 인덱스 + backlink 본문 치환. */
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
        val oldTitle = memoryRepository.findByUserIdAndId(userId, memoryId)?.title
        val memory = memoryService.update(userId, memoryId, expectedVersion, title, content, summary, folderId)
        if (embedding != null) embeddingDao.updateEmbedding(userId, memory.id, embedding)
        if (content != null) wikiLinkIndexer.reindex(userId, memory.id, content)
        if (title != null && oldTitle != null && oldTitle != title) {
            backlinkRenameService.rename(userId, oldTitle, title, except = memory.id)
        }
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
