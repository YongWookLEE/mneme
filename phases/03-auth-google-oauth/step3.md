# Step 3: session-csrf-headers-cors

> Task 3 of phase 03. 세션 쿠키 설정(Secure/HttpOnly/SameSite=Lax), CSRF 토큰(SPA 친화 CookieCsrfTokenRepository), 보안 헤더(CSP/HSTS/XFO/Referrer-Policy), CORS allowlist(`MNEME_FRONTEND_ORIGIN`).

## 작업

- application.yml: `server.servlet.session.cookie` 옵션
- SecurityConfig 확장: CSRF, 보안 헤더, CORS configurer
- `CorsConfig`: WebMvcConfigurer 또는 Spring Security CorsConfigurationSource

## Acceptance

- 빌드 + 스모크 + ktlint 통과
- 부팅 후 응답 헤더에 X-Frame-Options/X-Content-Type-Options/Referrer-Policy 포함

## 금지

- CORS allowed-origin에 `*` 와일드카드 사용 금지
- HSTS 운영 강제 활성화는 phase 30(도메인 확정)에서. 본 phase에서는 운영 프로파일에만 enable
