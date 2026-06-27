package com.mneme.security

import org.springframework.http.HttpStatus

/**
 * Mneme 백엔드 에러 코드 enum.
 *
 * 키는 영문 식별자(ERR_*). 사용자 노출 문구는 i18n 키로 분리되어 프론트엔드에서 번역.
 * 응답 본문에 한국어를 직접 싣지 않는다(`ApiErrorResponse.code` + 클라이언트 측 번역).
 */
enum class ApiError(
    val code: String,
    val status: HttpStatus,
    val messageKey: String,
) {
    BAD_REQUEST("ERR_BAD_REQUEST", HttpStatus.BAD_REQUEST, "error.bad-request"),
    UNAUTHORIZED("ERR_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN("ERR_FORBIDDEN", HttpStatus.FORBIDDEN, "error.forbidden"),
    NOT_FOUND("ERR_NOT_FOUND", HttpStatus.NOT_FOUND, "error.not-found"),
    CONFLICT("ERR_CONFLICT", HttpStatus.CONFLICT, "error.conflict"),
    RATE_LIMIT("ERR_RATE_LIMIT", HttpStatus.TOO_MANY_REQUESTS, "error.rate-limit"),
    QUOTA_EXCEEDED("ERR_QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "error.quota-exceeded"),
    INTERNAL("ERR_INTERNAL", HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),
}
