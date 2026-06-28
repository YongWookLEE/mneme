# Step 1 — Scaffolding + 엔티티 + 토큰 인증 필터

## 범위

- `com/mneme/oauth/` 패키지 신설
- 엔티티: `OAuthClient`(테이블 `oauth_clients` 매핑), `OAuthToken`(테이블 `oauth_tokens` 매핑)
- 리포지토리 2개
- `OAuthProperties`(access TTL 30분, refresh TTL 30일)
- `OAuthAccessTokenAuthenticationFilter`: `Authorization: Bearer <opaque>`이지만 `mn_` prefix가 아닌 토큰을 sha256으로 `oauth_tokens` lookup → SecurityContext

## Acceptance

- 컴파일 통과
- 기존 ApiKey Bearer는 `mn_` prefix 분기로 그대로 동작
