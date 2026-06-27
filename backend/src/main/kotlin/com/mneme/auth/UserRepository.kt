package com.mneme.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 사용자 리포지토리.
 *
 * 사용자 본인 정보 조회·갱신용. 다른 도메인 리포지토리와 달리 `userId`를 첫 인자로 받는 메서드가 없다 —
 * users 테이블 자체가 사용자 단위이므로 `id`가 곧 사용자 식별.
 */
@Repository
interface UserRepository : JpaRepository<User, UUID> {
    /**
     * Google subject로 사용자 조회(로그인 시 식별).
     *
     * @param googleSub Google OAuth subject
     * @return 사용자 또는 null
     */
    fun findByGoogleSub(googleSub: String): User?
}
