package com.mneme.mcp

import com.mneme.auth.UserRepository
import com.mneme.id.IdFactory
import com.mneme.id.PrefixedId
import com.mneme.memory.MemoryRepository
import com.mneme.memory.MemoryService
import com.mneme.security.AuthenticatedUserResolver
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * MCP `mn_*` 도구 단위 격리 회귀.
 *
 * 사용자 B 컨텍스트(`AuthenticatedUserResolver.currentUserId()` → userB)에서 사용자 A의 메모리 ID로
 * read/update/archive/restore/relations 호출 시 모두 404가 떨어지는지 검증한다. 서비스 레이어 단위로
 * mock을 채우며, REST/Streamable 통합 검증은 phase 15(client-validation)에서 수행한다.
 *
 * @author Mneme
 * @since phase 09
 */
class McpToolsIsolationTest {
    private val idFactory = IdFactory()
    private val userA: UUID = idFactory.newUuid()
    private val userB: UUID = idFactory.newUuid()
    private val memoryOfA: UUID = idFactory.newUuid()
    private val memoryExtId: String = PrefixedId(PrefixedId.Prefix.MEMORY, memoryOfA).format()

    private fun newTools(
        memoryService: MemoryService,
        memoryRepo: MemoryRepository = mockk(relaxed = true),
    ): MnemeTools {
        val resolver = mockk<AuthenticatedUserResolver>()
        every { resolver.currentUserId() } returns userB
        return MnemeTools(
            userResolver = resolver,
            userRepository = mockk(relaxed = true),
            memoryService = memoryService,
            memoryWriteFacade = mockk(relaxed = true),
            folderService = mockk(relaxed = true),
            tagService = mockk(relaxed = true),
            searchService = mockk(relaxed = true),
            memoryRepository = memoryRepo,
            memoryLinkRepository = mockk(relaxed = true),
        )
    }

    /** mn_read: 다른 사용자 메모리 ID → 404. */
    @Test
    fun `mn_read 는 다른 사용자에게 404`() {
        val memoryService = mockk<MemoryService>()
        every { memoryService.get(userB, memoryOfA) } throws ResponseStatusException(HttpStatus.NOT_FOUND)
        val tools = newTools(memoryService)

        shouldThrow<ResponseStatusException> {
            tools.read(memoryExtId)
        }.also { assert(it.statusCode == HttpStatus.NOT_FOUND) }
    }

    /** mn_archive: 다른 사용자 메모리 ID → 404. */
    @Test
    fun `mn_archive 는 다른 사용자에게 404`() {
        val memoryService = mockk<MemoryService>()
        every { memoryService.archive(userB, memoryOfA) } throws ResponseStatusException(HttpStatus.NOT_FOUND)
        val tools = newTools(memoryService)

        shouldThrow<ResponseStatusException> {
            tools.archive(memoryExtId)
        }.also { assert(it.statusCode == HttpStatus.NOT_FOUND) }
    }

    /** mn_restore: 다른 사용자 메모리 ID → 404. */
    @Test
    fun `mn_restore 는 다른 사용자에게 404`() {
        val memoryService = mockk<MemoryService>()
        every { memoryService.restore(userB, memoryOfA) } throws ResponseStatusException(HttpStatus.NOT_FOUND)
        val tools = newTools(memoryService)

        shouldThrow<ResponseStatusException> {
            tools.restore(memoryExtId)
        }.also { assert(it.statusCode == HttpStatus.NOT_FOUND) }
    }

    /** mn_relations: 다른 사용자 메모리 ID → 404 (memoryService.get 단계에서 차단). */
    @Test
    fun `mn_relations 는 다른 사용자에게 404`() {
        val memoryService = mockk<MemoryService>()
        every { memoryService.get(userB, memoryOfA) } throws ResponseStatusException(HttpStatus.NOT_FOUND)
        val tools = newTools(memoryService)

        shouldThrow<ResponseStatusException> {
            tools.relations(memoryExtId)
        }.also { assert(it.statusCode == HttpStatus.NOT_FOUND) }
    }

    /** mn_whoami: 인증된 사용자 본인의 정보만 반환(다른 사용자 데이터 노출 없음). */
    @Test
    fun `mn_whoami 는 본인 사용자만 반환`() {
        val resolver = mockk<AuthenticatedUserResolver>()
        every { resolver.currentUserId() } returns userB
        val userRepo = mockk<UserRepository>()
        val userBEntity = com.mneme.auth.User(id = userB, googleSub = "subB", email = "b@example.com")
        every { userRepo.findById(userB) } returns java.util.Optional.of(userBEntity)
        val tools =
            MnemeTools(
                userResolver = resolver,
                userRepository = userRepo,
                memoryService = mockk(relaxed = true),
                memoryWriteFacade = mockk(relaxed = true),
                folderService = mockk(relaxed = true),
                tagService = mockk(relaxed = true),
                searchService = mockk(relaxed = true),
                memoryRepository = mockk(relaxed = true),
                memoryLinkRepository = mockk(relaxed = true),
            )

        val result = tools.whoami()
        assert(result["email"] == "b@example.com")
        assert(result["user_ext_id"] == PrefixedId(PrefixedId.Prefix.USER, userB).format())
    }

    /** userA를 가장한 가짜 ext id로 호출해도 service 레이어에서 userB 컨텍스트로 격리됨을 확인. */
    @Test
    fun `다른 사용자 ext id 위장 호출도 userB 컨텍스트로 호출됨`() {
        val memoryService = mockk<MemoryService>()
        every { memoryService.get(userB, memoryOfA) } throws ResponseStatusException(HttpStatus.NOT_FOUND)
        val tools = newTools(memoryService)

        // userA의 메모리 ext id로 read 호출 → service는 userB 컨텍스트로 받아서 404
        shouldThrow<ResponseStatusException> {
            tools.read(memoryExtId)
        }
    }
}
