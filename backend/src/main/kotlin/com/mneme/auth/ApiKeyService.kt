package com.mneme.auth

import com.mneme.id.IdFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * API 키 도메인 서비스.
 *
 * 발급/목록/폐기/이름 수정/회전을 책임진다. CRITICAL: 모든 메서드 첫 인자 userId. 평문 키는 `issue`의 응답에서만 1회 노출.
 *
 * 14일 grace period(ADR-005)는 phase 08에서 도입. 본 step의 `rotate`는 즉시 폐기 + 새 키 발급.
 */
@Service
@Transactional
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val generator: ApiKeyGenerator,
    private val idFactory: IdFactory,
) {
    private val log = LoggerFactory.getLogger(ApiKeyService::class.java)

    /**
     * 신규 키 발급.
     *
     * @return [IssuedKey] — plaintext는 1회 노출, key는 저장된 엔티티.
     */
    fun issue(
        userId: UUID,
        name: String,
    ): IssuedKey {
        val generated = generator.generate()
        val key =
            ApiKey(
                id = idFactory.newUuid(),
                userId = userId,
                name = name,
                keyHash = generated.hash,
                prefix = generated.identifier,
            )
        apiKeyRepository.save(key)
        log.info("API key issued: userId={}, keyId={}, prefix={}", userId, key.id, key.prefix)
        return IssuedKey(plaintext = generated.plaintext, key = key)
    }

    /** 사용자 본인의 활성 키 목록. */
    @Transactional(readOnly = true)
    fun listActive(userId: UUID): List<ApiKey> = apiKeyRepository.findAllByUserIdAndRevokedAtIsNull(userId)

    /** 폐기. 존재하지 않거나 다른 사용자의 키면 false(권한 위반은 404로 매핑). */
    fun revoke(
        userId: UUID,
        keyId: UUID,
    ): Boolean {
        val key = apiKeyRepository.findByUserIdAndId(userId, keyId) ?: return false
        if (key.revokedAt != null) return true
        key.revokedAt = OffsetDateTime.now()
        log.info("API key revoked: userId={}, keyId={}", userId, key.id)
        return true
    }

    /** 이름 수정. */
    fun rename(
        userId: UUID,
        keyId: UUID,
        newName: String,
    ): Boolean {
        val key = apiKeyRepository.findByUserIdAndId(userId, keyId) ?: return false
        key.name = newName
        return true
    }

    /**
     * 회전 — 기존 키 즉시 폐기 + 같은 이름으로 신규 발급.
     * 14일 grace period는 phase 08에서 도입(`old_key_id` + `replaces_at` 필드 추가 후).
     */
    fun rotate(
        userId: UUID,
        keyId: UUID,
    ): IssuedKey? {
        val old = apiKeyRepository.findByUserIdAndId(userId, keyId) ?: return null
        if (old.revokedAt == null) {
            old.revokedAt = OffsetDateTime.now()
        }
        return issue(userId, old.name)
    }

    /** 발급 결과. */
    data class IssuedKey(
        val plaintext: String,
        val key: ApiKey,
    )
}
