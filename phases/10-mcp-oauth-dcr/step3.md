# Step 3 — Authorization Code 흐름

## 범위

- `GET /oauth/authorize?response_type=code&client_id=&redirect_uri=&state=&code_challenge=&code_challenge_method=S256`
  - 세션 미인증이면 `/oauth2/authorization/google`로 리다이렉트
  - 인증돼 있으면 redirect_uri로 `?code=...&state=...` 리다이렉트
  - 코드는 Caffeine `ConcurrentMap<String, Code>` (10분 TTL), payload: `{ userId, clientId, redirectUri, codeChallenge }`
- `POST /oauth/token` — body x-www-form-urlencoded
  - `grant_type=authorization_code&code=&redirect_uri=&client_id=&client_secret=&code_verifier=`
  - 검증: client 인증, code 일치, redirect_uri 일치, PKCE 검증, code 1회 사용
  - 응답: `{ "access_token":"<opaque>", "token_type":"Bearer", "expires_in":1800, "refresh_token":"<opaque>", "scope":"mcp" }`
  - 해시는 sha256, DB의 `oauth_tokens`에 저장
- `grant_type=refresh_token` 분기도 지원

## Acceptance

- curl로 register → authorize(세션 인증 가정 — 테스트는 직접 user/client 시드) → token 응답 정상
- `/sse` + `/mcp/message`에 OAuth access token으로 Bearer 인증 통과
