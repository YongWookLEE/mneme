package com.mneme.wiki

import com.mneme.memory.MemoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 메모리 제목이 바뀔 때 다른 메모리 본문 안의 `[[옛 제목]]`을 일괄 치환한다.
 *
 * `MemoryWriteFacade`에서 호출되며, 별도 빈으로 분리해 Spring AOP의
 * self-invocation 한계를 피한다(같은 클래스 안 메서드끼리 호출하면 `@Transactional`이 적용되지 않음).
 *
 * @author Mneme
 * @since phase 16
 */
@Service
class BacklinkRenameService(
    private val memoryRepository: MemoryRepository,
    private val wikiLinkIndexer: WikiLinkIndexer,
) {
    /** 본인 활성 메모리 본문에서 `[[oldTitle]]`을 `[[newTitle]]`로 치환하고 인덱스 재구축. except는 건너뜀. */
    @Transactional
    fun rename(
        userId: UUID,
        oldTitle: String,
        newTitle: String,
        except: UUID,
    ) {
        val pattern = "[[$oldTitle]]"
        val replacement = "[[$newTitle]]"
        val candidates = memoryRepository.findAllByUserIdAndArchivedAtIsNull(userId)
        for (m in candidates) {
            if (m.id == except) continue
            if (!m.content.contains(pattern)) continue
            m.content = m.content.replace(pattern, replacement)
            m.byteSize = m.content.toByteArray(Charsets.UTF_8).size
            m.updatedAt = OffsetDateTime.now()
            wikiLinkIndexer.reindex(userId, m.id, m.content)
        }
    }
}
