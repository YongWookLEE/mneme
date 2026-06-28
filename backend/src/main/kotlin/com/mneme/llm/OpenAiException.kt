package com.mneme.llm

/**
 * OpenAI 호출 실패. 상태 코드별로 호출자가 구분 처리할 수 있게 nested type 사용.
 */
sealed class OpenAiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    /** API 키 미설정 또는 401. */
    class Unauthorized(
        message: String,
    ) : OpenAiException(message)

    /** 429 또는 일일 한도 초과. */
    class RateLimited(
        message: String,
    ) : OpenAiException(message)

    /** 4xx 일반. */
    class BadRequest(
        message: String,
    ) : OpenAiException(message)

    /** 5xx 또는 네트워크. */
    class ServerError(
        message: String,
        cause: Throwable? = null,
    ) : OpenAiException(message, cause)
}
