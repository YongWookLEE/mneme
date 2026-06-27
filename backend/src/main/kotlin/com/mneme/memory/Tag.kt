package com.mneme.memory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * 태그 엔티티.
 *
 * 사용자별 자유 입력 태그. 이름은 소문자 저장(서비스에서 정규화). 한 메모리는 0~16개 태그를 가질 수 있다.
 */
@Entity
@Table(name = "tags")
class Tag(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
)
