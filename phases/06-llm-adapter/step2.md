# step 2 — embedding-service-cache

## 목적

`text-embedding-3-small` 1536d 임베딩 + Caffeine 캐시(같은 본문 재호출 방지).

## 범위

- `EmbeddingService.embed(text): FloatArray`.
- Caffeine 캐시: 텍스트 SHA-256 → FloatArray. 최대 5,000 엔트리, 30분 TTL.
- 호출 결과 토큰 사용량을 `UsageDailyService`로 위임(step 4에서 구현, 본 step은 인터페이스만).
- 단위 테스트: 캐시 히트 시 OpenAI 미호출.

## Acceptance

- `./gradlew :backend:test --tests "*EmbeddingService*"` 통과.
