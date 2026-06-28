package com.mneme.llm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID

/**
 * gpt-4o-mini 채팅 호출. 요약/폴더 분류/태그 제안.
 *
 * 실패 시 빈 결과를 반환해 메모리 저장은 막지 않는다(MVP). 사용량은 [UsageRecorder]에 위임.
 */
@Service
class ChatService(
    private val openAi: OpenAiClient,
    private val props: LlmProperties,
    private val usageRecorder: UsageRecorder,
    private val mapper: ObjectMapper = ObjectMapper(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val summarizePrompt: String by lazy { loadPrompt("llm/prompts/summarize.md") }
    private val classifyPrompt: String by lazy { loadPrompt("llm/prompts/classify-folder.md") }
    private val suggestTagsPrompt: String by lazy { loadPrompt("llm/prompts/suggest-tags.md") }

    /** 본문 한 줄 요약. 실패 시 null. */
    fun summarize(
        userId: UUID,
        content: String,
    ): String? = safeCall { call(userId, summarizePrompt, PromptGuard.fence(content)) }?.trim()?.take(120)

    /** 후보 경로 중 가장 적합한 폴더 path 선택. 실패 또는 미일치 시 null. */
    fun classifyFolder(
        userId: UUID,
        content: String,
        candidates: List<String>,
    ): String? {
        if (candidates.isEmpty()) return null
        val user = PromptGuard.fence(content) + "\n\nCandidates:\n" + candidates.joinToString("\n") { "- $it" }
        val raw = safeCall { call(userId, classifyPrompt, user) }?.trim() ?: return null
        return candidates.firstOrNull { it.equals(raw, ignoreCase = true) }
    }

    /** 최대 5개 태그 제안. 실패 또는 파싱 실패 시 빈 리스트. */
    fun suggestTags(
        userId: UUID,
        content: String,
    ): List<String> {
        val raw = safeCall { call(userId, suggestTagsPrompt, PromptGuard.fence(content)) } ?: return emptyList()
        return runCatching {
            val node = mapper.readTree(raw)
            if (!node.isArray) return emptyList()
            node
                .mapNotNull {
                    it
                        .asText()
                        .lowercase()
                        .take(32)
                        .takeIf(String::isNotBlank)
                }.distinct()
                .take(5)
        }.getOrElse {
            log.warn("태그 응답 JSON 파싱 실패: {}", raw.take(200))
            emptyList()
        }
    }

    private fun call(
        userId: UUID,
        system: String,
        user: String,
    ): String {
        val body =
            mapOf(
                "model" to props.chatModel,
                "temperature" to 0.2,
                "messages" to
                    listOf(
                        mapOf("role" to "system", "content" to system),
                        mapOf("role" to "user", "content" to user),
                    ),
            )
        val response = openAi.postChat(body)
        recordUsage(userId, response)
        return response
            .path("choices")
            .firstOrNull()
            ?.path("message")
            ?.path("content")
            ?.asText()
            ?: throw OpenAiException.ServerError("chat 응답 형식 오류")
    }

    private fun recordUsage(
        userId: UUID,
        response: JsonNode,
    ) {
        val usage = response.path("usage")
        val input = usage.path("prompt_tokens").asInt(0)
        val output = usage.path("completion_tokens").asInt(0)
        if (input > 0 || output > 0) {
            val date = clock.instant().atZone(ZoneOffset.UTC).toLocalDate()
            usageRecorder.recordChatTokens(userId, date, input, output)
        }
    }

    private fun <T> safeCall(block: () -> T): T? =
        try {
            block()
        } catch (e: OpenAiException) {
            log.warn("OpenAI 호출 실패, 메모리 저장은 계속: {}", e.message)
            null
        }

    private fun loadPrompt(path: String): String = ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
}
