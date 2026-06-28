# Step 2 — RFC 7591 DCR

## 범위

- `POST /oauth/register` — 공개. body는 `{ "redirect_uris": [...], "client_name": "..." }`. 응답은 `{ "client_id": "oac_...", "client_secret": "<opaque>", "redirect_uris": [...], "client_name": "..." }`.
- `client_secret`는 sha256만 저장, 응답에 1회 노출.
- 등록되는 client는 어떤 사용자에 종속되지 않는 "anonymous" 등록 — 다만 redirect_uri는 보존하며, 실제 사용자 매핑은 authorize 단계의 user 동의로 결정한다. (테이블 user_id는 NULL 허용 불가 → 시드 admin user 또는 컬럼 nullable로 변경. 본 phase에서는 user_id NULL을 허용하도록 V2 마이그레이션 추가.)

## Acceptance

- curl로 `/oauth/register` → 201 + client_id/client_secret 응답 + DB에 row
