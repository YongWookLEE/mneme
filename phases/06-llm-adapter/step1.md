# step 1 — deps-and-config

## 목적

OpenAI HTTP 클라이언트 베이스 + 설정 바인딩.

## 범위

- `backend/build.gradle.kts`에 `com.github.ben-manes.caffeine:caffeine` 추가.
- `mneme.openai.*` 속성을 `LlmProperties`로 바인딩.
- `OpenAiClient`(`RestClient` 래퍼): timeout, base URL, Authorization 헤더, JSON.
- `OpenAiException`: rate limit/quota/4xx/5xx 구분.

## 비범위

- 임베딩/채팅 호출 자체(step 2/3).
- Caffeine 캐시 빈(step 2).
- MemoryService 통합(step 4).

## Acceptance

- `./gradlew :backend:build` 통과.
- `MNEME_OPENAI_API_KEY` 비었을 때 `OpenAiClient`는 빈 등록 안 되거나 호출 시 명시 에러.
