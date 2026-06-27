package com.mneme.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 사용자 엔티티.
 *
 * Google OAuth로 가입한 멀티테넌트 사용자. `googleSub`은 Google 발급 subject(고유). 계정 삭제는
 * `deletedAt` soft delete 후 30일 유예. 데이터 격리는 모든 도메인 엔티티의 `userId` 외래키로 강제.
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "google_sub", nullable = false, unique = true)
    val googleSub: String,
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "locale", nullable = false)
    var locale: String = "ko",
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
