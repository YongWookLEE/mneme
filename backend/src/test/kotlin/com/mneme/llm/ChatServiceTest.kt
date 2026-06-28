package com.mneme.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.mneme.security.TokenQuotaGuard
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID

/**
 * ChatService 동작 검증.
 */
class ChatServiceTest {
    private val openAi = mockk<OpenAiClient>()
    private val recorder = mockk<UsageRecorder>(relaxed = true)
    private val quotaGuard = mockk<TokenQuotaGuard>(relaxed = true)
    private val mapper = ObjectMapper()
    private val service = ChatService(openAi, LlmProperties(apiKey = "test"), recorder, quotaGuard, null, mapper, Clock.systemUTC())

    private fun chatResponse(content: String): String =
        """
        {"choices":[{"message":{"content":"$content"}}],
         "usage":{"prompt_tokens":7,"completion_tokens":3}}
        """.trimIndent()

    @Test
    fun `summarize trims and limits length`() {
        every { openAi.postChat(any()) } returns mapper.readTree(chatResponse("  요약입니다.  "))
        val result = service.summarize(UUID.randomUUID(), "긴 본문")
        result shouldBe "요약입니다."
        verify { recorder.recordChatTokens(any(), any(), 7, 3) }
    }

    @Test
    fun `classifyFolder picks from candidates only`() {
        every { openAi.postChat(any()) } returns mapper.readTree(chatResponse("dev/kotlin"))
        val result = service.classifyFolder(UUID.randomUUID(), "코틀린 메모", listOf("dev/kotlin", "life/diary"))
        result shouldBe "dev/kotlin"
    }

    @Test
    fun `classifyFolder rejects invented path`() {
        every { openAi.postChat(any()) } returns mapper.readTree(chatResponse("dev/new-folder"))
        val result = service.classifyFolder(UUID.randomUUID(), "x", listOf("dev/kotlin"))
        result shouldBe null
    }

    @Test
    fun `suggestTags parses json array`() {
        every { openAi.postChat(any()) } returns mapper.readTree(chatResponse("[\\\"kotlin\\\",\\\"spring\\\"]"))
        val tags = service.suggestTags(UUID.randomUUID(), "x")
        tags shouldContainExactly listOf("kotlin", "spring")
    }

    @Test
    fun `openai failure returns null instead of throwing`() {
        every { openAi.postChat(any()) } throws OpenAiException.ServerError("boom")
        service.summarize(UUID.randomUUID(), "x") shouldBe null
        service.suggestTags(UUID.randomUUID(), "x") shouldBe emptyList()
    }
}
