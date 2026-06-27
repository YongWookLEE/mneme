# Step 5: handoff (사용자 개입 필요 안내)

> Task 5 of phase 03. 코드 측면 phase 03은 완료. **실제 Google 로그인 검증은 사용자가 GCP Console에서 OAuth 2.0 Client ID를 발급하고 `.env`에 입력해야 가능**. 본 step은 라이브 검증을 `blocked`로 표시하고 다음 phase(04 api-keys)로 큐.

## 사용자 액션 (라이브 검증 시 필요)

1. https://console.cloud.google.com/apis/credentials 접속
2. OAuth consent screen 구성 (External, App name "Mneme", scopes: `openid`, `email`, `profile`)
3. Credentials → Create Credentials → OAuth Client ID → Web application
4. Authorized redirect URI에 `${MNEME_BASE_URL}/login/oauth2/code/google` 등록 (로컬: `http://localhost:8080/login/oauth2/code/google`)
5. Client ID / Client Secret을 `deploy/.env`의 `MNEME_GOOGLE_OAUTH_CLIENT_ID` / `MNEME_GOOGLE_OAUTH_CLIENT_SECRET`에 입력
6. `docker compose up`으로 스택 재기동
7. `http://localhost:5173` 접속 → 로그인 버튼 클릭(추후 phase 11에서 UI 추가) 또는 직접 `http://localhost:8080/oauth2/authorization/google` 호출
8. Google 로그인 → callback 후 `users` 테이블에 새 row 생성 확인

## 작업

- HANDOFF / CHANGELOG / phases/03/index.json 갱신
- phase 03 → completed (코드), live verification은 `blocked_reason`에 사유 명시 가능 (단순화 위해 completed + 사용자 액션은 HANDOFF에만 기재)
- phase 04 (api-keys) 큐
