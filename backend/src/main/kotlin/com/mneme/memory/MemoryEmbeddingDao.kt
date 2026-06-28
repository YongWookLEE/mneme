package com.mneme.memory

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 메모리 임베딩(pgvector) 갱신용 DAO.
 *
 * JPA 엔티티에는 임베딩이 매핑되지 않는다(차원 1536 + pgvector 타입 + JPA의 SELECT 비효율). 외부 호출로
 * 임베딩을 계산한 뒤 본 DAO로 네이티브 UPDATE만 수행한다. user_id를 항상 WHERE에 포함해 격리 보장.
 */
@Component
class MemoryEmbeddingDao(
    @PersistenceContext private val em: EntityManager,
) {
    /**
     * 메모리 임베딩을 덮어쓴다. 사용자 본인 + 활성 메모리만 대상.
     *
     * @return 갱신된 row 수(0이면 해당 사용자의 활성 메모리가 아님)
     */
    @Transactional
    fun updateEmbedding(
        userId: UUID,
        memoryId: UUID,
        embedding: FloatArray,
    ): Int {
        val literal = embedding.joinToString(prefix = "[", postfix = "]", separator = ",")
        return em
            .createNativeQuery(
                "UPDATE memories SET embedding = CAST(:vec AS vector) WHERE id = :id AND user_id = :uid AND archived_at IS NULL",
            ).setParameter("vec", literal)
            .setParameter("id", memoryId)
            .setParameter("uid", userId)
            .executeUpdate()
    }
}
