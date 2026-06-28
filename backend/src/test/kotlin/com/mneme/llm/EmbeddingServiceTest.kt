package com.mneme.llm

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID

/**
 * Caffeine 캐시 동작 검증. 같은 텍스트는 OpenAI 미호출.
 */
class EmbeddingServiceTest {
    private val openAi = mockk<OpenAiClient>()
    private val recorder = mockk<UsageRecorder>(relaxed = true)
    private val props = LlmProperties(apiKey = "test", embeddingCacheMaxSize = 100, embeddingCacheTtlMinutes = 5)
    private val mapper = ObjectMapper()
    private val service =
        EmbeddingService(openAi, props, recorder, Clock.systemUTC()).also { it.init() }

    private fun fakeEmbedding(): String =
        buildString {
            append("""{"data":[{"embedding":[""")
            append((1..EmbeddingService.EXPECTED_DIM).joinToString(",") { "0.1" })
            append("]}],\"usage\":{\"total_tokens\":12}}")
        }

    @Test
    fun `같은 텍스트 두 번 호출 시 두 번째는 캐시 히트`() {
        every { openAi.postEmbedding(any()) } returns mapper.readTree(fakeEmbedding())
        val userId = UUID.randomUUID()

        val first = service.embed(userId, "hello world")
        val second = service.embed(userId, "hello world")

        first.size shouldBe EmbeddingService.EXPECTED_DIM
        second.size shouldBe EmbeddingService.EXPECTED_DIM
        verify(exactly = 1) { openAi.postEmbedding(any()) }
        verify(exactly = 1) { recorder.recordEmbedTokens(userId, any(), 12) }
    }

    @Test
    fun `다른 텍스트는 별도 호출`() {
        every { openAi.postEmbedding(any()) } returns mapper.readTree(fakeEmbedding())
        val userId = UUID.randomUUID()

        service.embed(userId, "alpha")
        service.embed(userId, "beta")

        verify(exactly = 2) { openAi.postEmbedding(any()) }
    }

    @Test
    fun `빈 문자열은 예외`() {
        try {
            service.embed(UUID.randomUUID(), "")
            error("예외 기대")
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }
}
