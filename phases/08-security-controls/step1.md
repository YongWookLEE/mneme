# step 1 — rate-limit-filter

사용자별 token bucket(메모리 내). Caffeine 캐시로 userId → bucket 매핑.

- 분당 일반(`MNEME_RATE_LIMIT_PER_MIN`, 기본 60) + 일당(`MNEME_RATE_LIMIT_PER_DAY`, 기본 5000) + 쓰기 분당(`MNEME_RATE_LIMIT_WRITE_PER_MIN`, 기본 20).
- POST/PATCH/DELETE는 쓰기 버킷도 함께 차감.
- 초과 시 429 + `ApiError.RATE_LIMIT`.
- 인증 없는 요청은 IP 기반 fallback 버킷(가벼운 60/min).

OncePerRequestFilter → SecurityContext의 userId 추출 후 차감 → 통과/차단.
