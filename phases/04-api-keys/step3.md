# P04 Step 3: api-key-rest

> Task 3 of phase 04. `/api/keys` REST 컨트롤러 + DTO. 인증 사용자(세션 또는 API 키 — 후자는 step 4에서 도입)에 대해서만 동작. 발급 응답에만 평문 1회 노출.

## 산출물

- `com.mneme.api.ApiKeyController`
  - `POST /api/keys` — issue
  - `GET /api/keys` — list active
  - `PATCH /api/keys/{extId}` — rename
  - `DELETE /api/keys/{extId}` — revoke
  - `POST /api/keys/{extId}/rotate` — rotate
- DTO: `IssueRequest`, `IssueResponse(extId, name, plaintext, prefix, createdAt)`, `KeyResponse(extId, name, prefix, lastUsedAt, createdAt)`
- 외부 ID 형식 `key_<base32>` 변환은 `IdFactory` + `PrefixedId.parse(..., "key")`
- 인증 컨텍스트는 Spring Security `Authentication.principal` 에서 `OAuth2User`의 sub로 User 조회

## Acceptance

- 빌드 + 스모크 + ktlint
- (라이브 테스트는 phase 11 또는 사용자 OAuth 검증 후)
