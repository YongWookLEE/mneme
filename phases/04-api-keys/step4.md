# P04 Step 4: api-key-auth-filter

> Task 4 of phase 04. `Authorization: Bearer mn_...` 헤더를 받아 prefix 조회 → keyHash 검증 → 일치 시 SecurityContext에 사용자 인증 주입. MCP 클라이언트(REST API 키 사용)와 자동화 스크립트가 이 필터로 인증된다.

## 산출물

- `com.mneme.security.ApiKeyAuthenticationFilter` (`OncePerRequestFilter` 상속)
- `com.mneme.security.ApiKeyAuthenticationToken` (Authentication 구현)
- SecurityConfig에 필터 등록(OAuth2 흐름과 병행)
- 인증 성공 시 `key.lastUsedAt` 비동기 갱신은 후속(여기선 즉시 갱신)

## Acceptance

- 빌드 + 스모크 + ktlint
- Bearer 헤더 없으면 401 (인증 실패 → 보호 엔드포인트 접근 시)
