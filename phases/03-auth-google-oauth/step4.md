# Step 4: i18n-error-codes

> Task 4 of phase 03. 백엔드 에러 코드 식별자(`ERR_*`)와 한국어 메시지 분리. ProblemDetail(RFC 7807) 응답을 기본 포맷으로. 전역 예외 핸들러 1개로 시작 — 추가 케이스는 후속 phase에서.

## 작업

- `com.mneme.security.ApiError`: 도메인 식별자 enum
- `com.mneme.security.GlobalExceptionHandler`: `@ControllerAdvice` — IllegalArgumentException/AccessDeniedException 매핑
- `backend/src/main/resources/i18n/errors.properties` + `errors_ko.properties`: 키-한국어 매핑

## Acceptance

- 빌드 + 스모크 + ktlint 통과
- 메시지 번들 로드 확인(빈 컨텍스트에서 MessageSource bean이 messages를 한국어로 반환)

## 금지

- 백엔드 응답 본문에 한국어 메시지를 하드코딩하지 마라. 프런트가 코드를 받아 i18n 번역.
- 예외 응답에 stack trace 포함 금지
