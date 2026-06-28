package com.mneme.wiki

import com.mneme.id.IdFactory
import com.mneme.id.PrefixedId
import com.mneme.memory.MemoryLink
import com.mneme.memory.MemoryLinkRepository
import com.mneme.memory.MemoryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 메모리 저장 트랜잭션 안에서 호출되어 본문 안 `[[link]]`를 파싱하고
 * `memory_links` 인덱스를 source 단위로 비우고 다시 채운다.
 *
 * 격리: 모든 메서드 첫 인자 userId. 권한 없는 메모리는 무시.
 *
 * @author Mneme
 * @since phase 16
 */
@Component
class WikiLinkIndexer(
    private val memoryRepository: MemoryRepository,
    private val memoryLinkRepository: MemoryLinkRepository,
    private val idFactory: IdFactory,
) {
    /**
     * sourceId 메모리의 forward 링크를 본문에 맞게 동기화.
     *
     * 1. 본문 파싱
     * 2. `[[mem_…]]` ext id → 본인 메모리만 target_id 매칭
     * 3. `[[제목]]` → 동일 사용자 메모리 제목으로 매칭(없으면 broken: target_id=null)
     * 4. 기존 source 링크 전부 삭제 후 신규 insert (단순/안전)
     */
    @Transactional
    fun reindex(
        userId: UUID,
        sourceId: UUID,
        content: String,
    ) {
        memoryLinkRepository.deleteAllByUserIdAndSourceId(userId, sourceId)
        val parsed = WikiLinkParser.parse(content)
        if (parsed.isEmpty()) return
        for (link in parsed) {
            val targetId =
                when {
                    link.memoryExtId != null ->
                        runCatching { PrefixedId.parse(link.memoryExtId, PrefixedId.Prefix.MEMORY).uuid }
                            .getOrNull()
                            ?.let { memoryRepository.findByUserIdAndId(userId, it)?.id }
                    link.titleHint != null ->
                        memoryRepository.findByUserIdAndTitleAndArchivedAtIsNull(userId, link.titleHint)?.id
                    else -> null
                }
            val entity =
                MemoryLink(
                    id = idFactory.newUuid(),
                    userId = userId,
                    sourceId = sourceId,
                    targetId = targetId,
                    targetLabel = link.titleHint ?: (link.memoryExtId ?: link.rawText),
                    kind = "wiki",
                )
            memoryLinkRepository.save(entity)
        }
    }
}
