package com.mneme.llm

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * OpenAI HTTP API 직접 호출 래퍼. embedding/chat 2개 엔드포인트만 노출한다(ADR-020).
 *
 * - 키가 비어 있으면 호출 시 [OpenAiException.Unauthorized]로 실패.
 * - 429는 [OpenAiException.RateLimited], 5xx는 [OpenAiException.ServerError].
 * - 응답을 [JsonNode]로 반환 — 도메인 매핑은 호출자(서비스)가 담당.
 */
@Component
class OpenAiClient(
    private val props: LlmProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl(props.baseUrl)
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    val timeout = Duration.ofSeconds(props.requestTimeoutSeconds)
                    setConnectTimeout(timeout)
                    setReadTimeout(timeout)
                },
            ).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    /** POST `/v1/embeddings`. 호출자가 model/input을 포함한 body를 만든다. */
    fun postEmbedding(body: Map<String, Any>): JsonNode = post("/v1/embeddings", body)

    /** POST `/v1/chat/completions`. */
    fun postChat(body: Map<String, Any>): JsonNode = post("/v1/chat/completions", body)

    private fun post(
        path: String,
        body: Map<String, Any>,
    ): JsonNode {
        if (props.apiKey.isBlank()) {
            throw OpenAiException.Unauthorized("MNEME_OPENAI_API_KEY 미설정")
        }
        return try {
            restClient
                .post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${props.apiKey}")
                .body(body)
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw OpenAiException.ServerError("응답 본문이 비었습니다")
        } catch (e: HttpClientErrorException.Unauthorized) {
            throw OpenAiException.Unauthorized("OpenAI 401: ${e.statusText}")
        } catch (e: HttpClientErrorException.TooManyRequests) {
            throw OpenAiException.RateLimited("OpenAI 429: ${e.statusText}")
        } catch (e: HttpClientErrorException) {
            log.warn("OpenAI 4xx {} on {}: {}", e.statusCode.value(), path, e.responseBodyAsString.take(500))
            throw OpenAiException.BadRequest("OpenAI ${e.statusCode.value()}")
        } catch (e: HttpServerErrorException) {
            throw OpenAiException.ServerError("OpenAI ${e.statusCode.value()}", e)
        } catch (e: ResourceAccessException) {
            throw OpenAiException.ServerError("OpenAI 네트워크 오류", e)
        }
    }
}
