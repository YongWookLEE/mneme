# Step 1: spring-security-base

> Task 1 of phase 03. spring-boot-starter-security + spring-boot-starter-oauth2-client 의존성 도입. 기본 SecurityFilterChain: `/actuator/health`, `/actuator/health/**`만 익명 허용. 나머지는 인증 필요. 인증되지 않은 요청에 대한 401 응답. OAuth2 provider 설정은 step 2에서.

## 작업

- `backend/build.gradle.kts`에 의존성 추가
- `com.mneme.security` 패키지에 `SecurityConfig` 클래스: `SecurityFilterChain` 빈 정의
- application.yml에 `spring.security.user` 자동 사용자 비활성화(생성되는 임시 패스워드 방지)
- smoke test가 깨지지 않도록(공개 경로만 호출하므로 OK)

## Acceptance

- `./gradlew :backend:build :backend:test :backend:ktlintCheck` 모두 통과
- 부팅 시 Spring Security가 적용됐다는 로그 출력
- `/actuator/health`는 인증 없이 200, `/api/...`는 401

## 금지

- formLogin/httpBasic 활성화 금지(OAuth만)
- /actuator의 다른 엔드포인트(metrics/info 등) 공개 금지
