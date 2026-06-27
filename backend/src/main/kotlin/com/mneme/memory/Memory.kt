package com.mneme.memory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 메모리 엔티티.
 *
 * 마크다운 본문 1개 = 1 메모리. `embedding`(vector(1536))과 `tsv`(tsvector)는 native column으로 V1__init.sql에 존재하지만
 * 본 phase에서는 JPA에 매핑하지 않는다(phase 06 LLM 어댑터에서 PGobject/custom type으로 도입). 본문 저장 시점에 `byteSize`로 256KB 상한 검사.
 *
 * 낙관적 락(ADR-016): `@Version` `Long` 필드로 충돌 감지. 본문 수정은 서비스 트랜잭션 안에서 처리되며, 충돌 시 JPA `OptimisticLockException` →
 * 컨트롤러에서 409 + 두 버전 응답(phase 11에서 매핑).
 */
@Entity
@Table(name = "memories")
class Memory(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "folder_id", nullable = false)
    var folderId: UUID,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "content", nullable = false)
    var content: String,
    @Column(name = "summary")
    var summary: String? = null,
    @Column(name = "source_uri")
    var sourceUri: String? = null,
    @Column(name = "byte_size", nullable = false)
    var byteSize: Int,
    @Column(name = "model_version", nullable = false)
    var modelVersion: String = "text-embedding-3-small@1",
    @Column(name = "archived_at")
    var archivedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
)
