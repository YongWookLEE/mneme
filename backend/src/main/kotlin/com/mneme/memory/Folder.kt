package com.mneme.memory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 폴더 엔티티.
 *
 * 메모리의 단일 소속 컨테이너(파일 시스템 비유). materialized path(`path`)로 정규화 경로를 저장해 검색·이동을
 * O(1) 또는 prefix 매칭으로 처리한다. ADR-013에 따라 폴더는 1:N(메모리), 태그는 N:M.
 *
 * 양방향 매핑은 두지 않고 모든 쿼리는 `userId + parentId/path` 조합으로 명시적 수행.
 */
@Entity
@Table(name = "folders")
class Folder(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(name = "path", nullable = false)
    var path: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
