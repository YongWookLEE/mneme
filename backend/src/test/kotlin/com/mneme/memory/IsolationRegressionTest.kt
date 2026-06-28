package com.mneme.memory

import com.mneme.auth.ApiKey
import com.mneme.auth.ApiKeyRepository
import com.mneme.auth.ApiKeyService
import com.mneme.id.IdFactory
import com.mneme.observability.AuditPublisher
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 사용자 데이터 격리 회귀 테스트(서비스 레이어 베이스).
 *
 * **MVP 격리 정책(ADR-009)의 첫 자동 회귀**: 사용자 A의 리소스 ID로 사용자 B 컨텍스트에서 접근 시 NOT_FOUND가 반환되는지 검증한다.
 * 본 step에서는 서비스 계층 단위 테스트로 시작한다. 컨트롤러·MCP 도구 전수 검증은 phase 08(security-controls)에서 Testcontainers 호환 이슈 해결 후
 * `@SpringBootTest` + `TestRestTemplate` 회귀로 확장한다.
 *
 * 신규 엔드포인트 추가 시 본 클래스에 케이스를 추가하는 것이 코드 리뷰 체크리스트 항목(CONTRIBUTING.md / SECURITY.md 갱신은 phase 08).
 */
class IsolationRegressionTest {
    private val idFactory = IdFactory()
    private val userA: UUID = idFactory.newUuid()
    private val userB: UUID = idFactory.newUuid()

    /**
     * FolderService: 사용자 A의 폴더 ID로 사용자 B가 조회 → 404.
     */
    @Test
    fun `folder 조회는 다른 사용자에게 404`() {
        val repository = mockk<FolderRepository>()
        val folderId = idFactory.newUuid()
        every { repository.findByUserIdAndId(userB, folderId) } returns null
        val service = FolderService(repository, idFactory)

        shouldThrow<ResponseStatusException> {
            service.get(userB, folderId)
        }.also { assertNotFound(it.statusCode) }
    }

    /**
     * MemoryService: 사용자 A의 메모리 ID로 사용자 B가 조회 → 404.
     */
    @Test
    fun `memory 조회는 다른 사용자에게 404`() {
        val memoryRepo = mockk<MemoryRepository>()
        val folderRepo = mockk<FolderRepository>()
        val memoryId = idFactory.newUuid()
        every { memoryRepo.findByUserIdAndId(userB, memoryId) } returns null
        val service = MemoryService(memoryRepo, folderRepo, idFactory)

        shouldThrow<ResponseStatusException> {
            service.get(userB, memoryId)
        }.also { assertNotFound(it.statusCode) }
    }

    /**
     * TagService: 사용자 A의 메모리에 사용자 B가 태그 부착 → 404.
     */
    @Test
    fun `tag attach 는 다른 사용자 메모리에 404`() {
        val tagRepo = mockk<TagRepository>()
        val memoryRepo = mockk<MemoryRepository>()
        val memoryTagRepo = mockk<MemoryTagRepository>()
        val memoryId = idFactory.newUuid()
        every { memoryRepo.findByUserIdAndId(userB, memoryId) } returns null
        val service = TagService(tagRepo, memoryRepo, memoryTagRepo, idFactory)

        shouldThrow<ResponseStatusException> {
            service.attach(userB, memoryId, "hello")
        }.also { assertNotFound(it.statusCode) }
    }

    /**
     * ApiKeyService: 사용자 A의 키 ID를 사용자 B가 폐기 시도 → false(컨트롤러가 404로 변환).
     */
    @Test
    fun `api key revoke 는 다른 사용자에게 false`() {
        val apiKeyRepo = mockk<ApiKeyRepository>()
        val keyId = idFactory.newUuid()
        every { apiKeyRepo.findByUserIdAndId(userB, keyId) } returns null
        val service = ApiKeyService(apiKeyRepo, mockk(relaxed = true), idFactory, mockk<AuditPublisher>(relaxed = true))

        val ok = service.revoke(userB, keyId)
        if (ok) error("isolation breach — revoke returned true for non-owner")
    }

    /**
     * MemoryService: archive 도 동일 격리.
     */
    @Test
    fun `memory archive 는 다른 사용자에게 404`() {
        val memoryRepo = mockk<MemoryRepository>()
        val folderRepo = mockk<FolderRepository>()
        val memoryId = idFactory.newUuid()
        every { memoryRepo.findByUserIdAndIdAndArchivedAtIsNull(userB, memoryId) } returns null
        val service = MemoryService(memoryRepo, folderRepo, idFactory)

        shouldThrow<ResponseStatusException> {
            service.archive(userB, memoryId)
        }.also { assertNotFound(it.statusCode) }
    }

    /**
     * 사용자가 자신의 키만 조회 — 다른 사용자 키가 새지 않는지 베이스라인.
     */
    @Test
    fun `api key list 는 본인 키만 반환`() {
        val apiKeyRepo = mockk<ApiKeyRepository>()
        val ownKey = mockk<ApiKey>(relaxed = true)
        every { apiKeyRepo.findAllByUserIdAndRevokedAtIsNull(userB) } returns listOf(ownKey)
        val service = ApiKeyService(apiKeyRepo, mockk(relaxed = true), idFactory, mockk<AuditPublisher>(relaxed = true))

        val result = service.listActive(userB)
        if (result.size != 1 || result[0] !== ownKey) error("isolation breach")
    }

    private fun assertNotFound(actual: org.springframework.http.HttpStatusCode) {
        if (actual != HttpStatus.NOT_FOUND) {
            error("status was $actual, expected 404")
        }
    }
}
