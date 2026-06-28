package com.mneme.wiki

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 폴더별 LLM 합성 인덱스(요약 + 마크다운 본문).
 *
 * 사용자가 폴더에 들어갔을 때 "이 폴더가 어떤 내용인지" 한 눈에 보여준다.
 * 폴더 1개당 1행. `folder_id`가 PK이자 외래키.
 *
 * @author Mneme
 * @since phase 21
 */
@Entity
@Table(name = "folder_indexes")
class FolderIndex(
    @Id
    @Column(name = "folder_id", nullable = false, updatable = false)
    val folderId: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "summary", nullable = false)
    var summary: String,
    @Column(name = "body", nullable = false)
    var body: String,
    @Column(name = "memory_count", nullable = false)
    var memoryCount: Int = 0,
    @Column(name = "generated_at", nullable = false)
    var generatedAt: OffsetDateTime = OffsetDateTime.now(),
)
