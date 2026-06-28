# Step 2 — MCP 도구 인증 연동

## 범위

- MCP 엔드포인트(`/mcp` 등)는 기존 `ApiKeyAuthenticationFilter`를 그대로 통과한다 (Spring Security 체인 동일).
- `AuthenticatedUserResolver.currentUserId()`를 도구 메서드에서 호출 가능하도록 — `SecurityContextHolder`가 도구 dispatch 시점에 살아 있음을 확인.
- `SecurityConfig`에 `/mcp/**` 명시(현재 anyRequest().authenticated() 로 이미 보호되지만 가독성 위해 주석 추가).

## Acceptance

- MnemeToolsTest에서 `ApiKeyAuthenticationToken`을 SecurityContext에 주입한 뒤 도구 호출 → 정상 사용자 격리.
