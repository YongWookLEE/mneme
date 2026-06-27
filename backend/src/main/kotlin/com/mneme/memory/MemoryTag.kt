package com.mneme.memory

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

/**
 * 메모리 ↔ 태그 N:M 조인 테이블.
 *
 * 별도의 자동 증가 PK 없이 (memoryId, tagId) 복합 PK. 한쪽 삭제 시 CASCADE.
 * `userId`는 양 엔티티에 이미 있으므로 본 조인 테이블엔 두지 않는다(격리는 join 시 강제).
 */
@Entity
@Table(name = "memory_tags")
class MemoryTag(
    @EmbeddedId
    val id: MemoryTagId,
) {
    /**
     * 메모리 ID + 태그 ID 복합 키.
     */
    @Embeddable
    data class MemoryTagId(
        @Column(name = "memory_id", nullable = false, updatable = false)
        val memoryId: UUID,
        @Column(name = "tag_id", nullable = false, updatable = false)
        val tagId: UUID,
    ) : Serializable
}
