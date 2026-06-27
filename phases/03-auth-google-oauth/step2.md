# Step 2: google-oauth2-client

> Task 2 of phase 03. Google OAuth2 client 등록(환경 변수 조건부) + 성공 핸들러(`OAuth2LoginSuccessHandler`) — 첫 로그인 시 `users` 테이블 upsert. `MNEME_GOOGLE_OAUTH_CLIENT_ID`가 비어 있으면 OAuth2 빈을 생성하지 않아 앱은 정상 부팅하되 로그인 시도만 실패한다.

## 작업

1. `application.yml`에 Google OAuth registration (env 보간, 비어 있으면 빈 문자열)
2. `OAuth2LoginSuccessHandler`: `googleSub` upsert → User. `UserRepository` 사용
3. `SecurityConfig.oauth2Login {}` 블록 추가, successHandler 주입 (조건부)
4. `OAuth2ConditionConfig`: `MNEME_GOOGLE_OAUTH_CLIENT_ID` 존재 여부로 OAuth2 활성화 게이트
5. WebMvcTest + `oauth2Login()` mock으로 핸들러 단위 테스트

## Acceptance

- `./gradlew :backend:build :backend:test :backend:ktlintCheck` 모두 통과
- 환경 변수 없이도 앱 부팅(스모크 테스트 통과)
- 환경 변수 있을 때만 OAuth 등록이 활성화됨

## 금지

- 운영 코드에 client-secret 하드코딩 금지
- 더미 client-id로 OAuth 흐름을 강제 활성화하지 마라
