package com.mneme.security

import org.slf4j.LoggerFactory
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

/**
 * 전역 예외 핸들러.
 *
 * 응답은 RFC 7807 ProblemDetail. `type`은 `urn:mneme:error:{ApiError.code}`,
 * `title`은 i18n 키(클라이언트가 번역), `detail`은 빈 문자열(메모리 본문/PII 누출 방지).
 * stack trace는 본문에 포함하지 않는다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail {
        log.debug("400 bad request", ex)
        return problem(ApiError.BAD_REQUEST)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthn(ex: AuthenticationException): ProblemDetail {
        log.debug("401 unauthorized", ex)
        return problem(ApiError.UNAUTHORIZED)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAuthz(ex: AccessDeniedException): ProblemDetail {
        log.debug("403 forbidden", ex)
        return problem(ApiError.FORBIDDEN)
    }

    @ExceptionHandler(Exception::class)
    fun handleInternal(ex: Exception): ProblemDetail {
        log.error("500 internal", ex)
        return problem(ApiError.INTERNAL)
    }

    private fun problem(err: ApiError): ProblemDetail {
        val pd = ProblemDetail.forStatus(err.status)
        pd.type = URI.create("urn:mneme:error:${err.code}")
        pd.title = err.messageKey
        pd.setProperty("code", err.code)
        return pd
    }
}
