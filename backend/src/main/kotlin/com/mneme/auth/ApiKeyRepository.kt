package com.mneme.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * API 키 리포지토리.
 *
 * 평문 키 검증 흐름: 클라이언트 헤더의 키 → 앞 8자 prefix 추출 → `findAllByPrefixAndRevokedAtIsNull` 후보 조회 →
 * 각 후보의 keyHash와 sha256 비교 → 일치 키의 userId 확정. 본 prefix 조회는 격리의 예외(인증 단계라 userId 미상)지만
 * 후속 검증(keyHash 일치)에서 격리가 회복된다.
 */
@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {
    /**
     * 사용자 본인의 활성 키 목록.
     *
     * @param userId 사용자 ID
     * @return 폐기되지 않은 키 목록 (생성일 최신순은 호출자에서 정렬)
     */
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<ApiKey>

    /**
     * 사용자 본인의 단건 키.
     *
     * @param userId 사용자 ID
     * @param id 키 ID
     * @return 키 또는 null
     */
    fun findByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): ApiKey?

    /**
     * 평문 키 검증용 후보 조회. **격리 예외** — userId 없이 prefix로만 조회.
     * 호출자는 반드시 후보의 keyHash와 sha256 비교 후에만 사용해야 한다.
     *
     * @param prefix 키 앞 8자
     * @return 폐기되지 않은 후보 목록
     */
    fun findAllByPrefixAndRevokedAtIsNull(prefix: String): List<ApiKey>
}
