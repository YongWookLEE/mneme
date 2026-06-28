package com.mneme.llm

import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.util.UUID

/**
 * OpenAI text-embedding-3-small(1536d) 호출 + 텍스트 SHA-256 기반 Caffeine 캐시.
 *
 * 본문이 동일하면 같은 임베딩이므로 캐시 히트 = OpenAI 미호출. 사용자별 분리는 필요 없다(텍스트가 같으면
 * 임베딩도 같다 + 모델 결정적). 사용자별 토큰 집계는 [UsageRecorder]에 위임한다.
 */
@Service
class EmbeddingService(
    private val openAi: OpenAiClient,
    private val props: LlmProperties,
    private val usageRecorder: UsageRecorder,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var cache: Cache<String, FloatArray>

    @PostConstruct
    fun init() {
        cache =
            Caffeine
                .newBuilder()
                .maximumSize(props.embeddingCacheMaxSize)
                .expireAfterAccess(Duration.ofMinutes(props.embeddingCacheTtlMinutes))
                .recordStats()
                .build()
    }

    /**
     * 텍스트 임베딩. 캐시 히트 시 OpenAI 호출 없음.
     *
     * @return 1536 길이 FloatArray
     */
    fun embed(
        userId: UUID,
        text: String,
    ): FloatArray {
        require(text.isNotBlank()) { "임베딩 텍스트는 비어 있을 수 없습니다" }
        val key = sha256Hex(text)
        cache.getIfPresent(key)?.let { return it }

        val response = openAi.postEmbedding(mapOf("model" to props.embedModel, "input" to text))
        val vector = parseVector(response)
        val tokens = response.path("usage").path("total_tokens").asInt(0)
        if (tokens > 0) {
            usageRecorder.recordEmbedTokens(userId, clock.instant().atZone(ZoneOffset.UTC).toLocalDate(), tokens)
        }
        cache.put(key, vector)
        return vector
    }

    /** 캐시 통계(테스트·관측용). */
    fun cacheStats(): String = cache.stats().toString()

    private fun parseVector(node: JsonNode): FloatArray {
        val arr =
            node.path("data").firstOrNull()?.path("embedding")
                ?: throw OpenAiException.ServerError("embedding 응답 형식 오류")
        if (!arr.isArray) throw OpenAiException.ServerError("embedding이 배열이 아님")
        val out = FloatArray(arr.size())
        for (i in 0 until arr.size()) out[i] = arr[i].floatValue()
        if (out.size != EXPECTED_DIM) {
            log.warn("embedding 차원 불일치: ${out.size}d (기대 ${EXPECTED_DIM}d)")
        }
        return out
    }

    private fun sha256Hex(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val EXPECTED_DIM = 1536
    }
}
