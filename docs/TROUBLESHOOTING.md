# Mneme 트러블슈팅

자주 발생하는 문제와 해결 방법.

## 부팅

### 백엔드 컨테이너가 즉시 종료된다 (exit 1)

**증상**: `docker compose ps`에 `backend` 상태가 `Exited`.

원인 후보 + 진단:

```bash
docker compose -f deploy/docker-compose.yml logs backend --tail 100
```

자주 보이는 메시지:

| 로그 메시지 | 원인 | 해결 |
|---|---|---|
| `Connection refused ... postgres:5432` | postgres가 아직 안 떴음 | 30초 대기 후 `docker compose up -d backend` |
| `Migrating schema "public" to version "X" ... Failed` | Flyway forward-only 위반 (V2 등을 손으로 고침) | 마이그레이션 히스토리 복구 또는 새 V<N+1>로 보정 |
| `Application failed to start ... InvalidConfigurationPropertyValueException` | `.env`에서 숫자형 변수를 빈 문자열로 둠 | 기본값 그대로 두거나 숫자 채우기 |
| `BeanCreationException ... GoogleClientRegistration` | OAuth 클라이언트 ID/Secret이 빈 값 | `.env`에 정확한 값 채우기 |

### postgres 컨테이너가 healthy가 안 된다

```bash
docker compose -f deploy/docker-compose.yml logs postgres
```

`Permission denied`가 보이면 볼륨 권한 문제. `docker volume rm mneme_postgres_data` 후 다시 띄우면 됨(데이터 초기화 주의).

### 프론트엔드 404 또는 502

- **502**: Caddy가 frontend-dev에 도달 못 함. `docker compose ps`로 frontend-dev `Up`인지 확인.
- **404**: Vite dev 서버가 다른 포트 듣는지 확인 (`docker compose logs frontend-dev`).

## 인증

### Google OAuth 로그인 후 "redirect_uri_mismatch"

GCP Console의 OAuth 클라이언트에 등록된 redirect URI와 실제 호출 URI가 다릅니다.

- 로컬: `http://localhost:8080/login/oauth2/code/google` (포트 8080)
- 운영: `https://your-domain.com/login/oauth2/code/google` (https + 도메인)

여러 URI를 동시에 등록할 수 있으니 두 개 다 넣어도 됩니다.

### API 키로 호출하니 401/302가 떨어진다

체크리스트:

```bash
# 1) 키가 실제로 DB에 있는지
docker compose exec postgres psql -U mneme -d mneme -c \
  "SELECT prefix, revoked_at FROM api_keys ORDER BY created_at DESC LIMIT 5;"

# 2) 키가 만료/폐기된 것은 아닌지
#    revoked_at이 NULL이어야 함

# 3) 키 해시가 평문과 일치하는지 (셸에서 직접 계산)
echo -n "mn_yourplaintext" | shasum -a 256
# 위 결과가 key_hash 컬럼과 같아야 함 (hex)
```

### MCP `/sse` 호출 시 302가 떨어진다

`Authorization: Bearer <키>` 헤더가 누락됐거나 잘못된 헤더 이름입니다. curl로 검증:

```bash
curl -sN -H "Authorization: Bearer mn_..." -H "Accept: text/event-stream" \
  --max-time 5 http://localhost:8080/sse
```

`HTTP/1.1 200 OK` + `data: /mcp/message?sessionId=...`가 보이면 정상.

## MCP 클라이언트

### Codex CLI: `error: unexpected argument '--header'`

`--header` 옵션이 없습니다. 대신:

```bash
export MNEME_API_KEY="mn_..."
codex mcp add mneme --url http://localhost:8080/sse --bearer-token-env-var MNEME_API_KEY
```

또는 OAuth:

```bash
codex mcp add mneme --url http://localhost:8080/sse
codex mcp login mneme
```

### Claude Desktop 재시작했는데 도구가 안 보인다

1. `~/Library/Application Support/Claude/claude_desktop_config.json`이 valid JSON인지 확인 (`jq . config.json`).
2. **Claude Desktop을 완전 종료** (메뉴에서 Quit, ⌘Q는 트레이만 닫을 수 있음).
3. 다시 열고 새 대화에서 도구 아이콘 확인.

### ChatGPT Developer mode에서 `localhost:8080`을 못 봄

ChatGPT는 외부 클라우드에서 동작하므로 `localhost`는 자기 자신을 가리킵니다. 외부로 노출하세요:

```bash
# ngrok
ngrok http 8080
# 또는 cloudflared
cloudflared tunnel --url http://localhost:8080
```

발급된 공개 URL을 MCP server URL로 입력.

## 데이터

### "본문이 256KB를 초과할 수 없습니다" 에러

메모리 1건의 본문 상한입니다. 큰 글은 여러 메모리로 쪼개거나 attachment(phase 35)를 기다리세요.

### Import zip에서 메모리가 안 들어와요

- zip 안에 `.md` 파일이 있는지 확인 (다른 확장자는 무시됨)
- 같은 폴더·동일 제목은 `conflictExtId`로 표시됨. 충돌 항목은 default `skip`이라 적용 시 건너뜀. `replace` 또는 `create-new`로 바꿔야 함

### Search가 빈 결과만 반환

원인 후보:

- 임베딩이 생성되지 않음 → `usage_daily.embed_tokens`가 0이면 OpenAI 호출 실패. `MNEME_OPENAI_API_KEY` 확인.
- 임베딩 백필 필요 → 현재는 자동 백필 잡 없음. 메모리 본문을 PATCH로 다시 저장하면 재계산됨.

## LLM

### 매번 "OpenAI 호출 실패, 메모리 저장은 계속"

- 키가 만료/비활성: <https://platform.openai.com/api-keys>에서 status 확인
- 잔액 부족: <https://platform.openai.com/account/billing>
- rate limit: 백엔드 로그에서 `429`가 보이면 OpenAI 측 한도. 잠시 후 재시도.

### 토큰 한도 초과

`/usage` 페이지에서 일일 한도 확인. `.env`에서 `MNEME_TOKEN_LIMIT_EMBED_PER_DAY` / `MNEME_TOKEN_LIMIT_LLM_PER_DAY` 조정.

## 성능

### pgvector 검색이 느림

`memories` 수가 1만 건을 넘으면 ivfflat 인덱스 리빌드 필요:

```sql
REINDEX INDEX memories_embedding_ivfflat_idx;
ANALYZE memories;
```

### 백엔드 메모리 사용량 증가

JVM 기본 힙으로는 4GB 컨테이너에서 1GB 정도까지 늘 수 있습니다. 제한하려면 `deploy/docker-compose.yml`의 backend에 환경변수 추가:

```yaml
environment:
  JAVA_OPTS: "-Xmx512m -XX:MaxRAMPercentage=50"
```

## 그래도 안 되면

- GitHub Issues에 로그(평문 키·토큰은 절대 포함하지 마세요) + 환경(OS · Docker 버전 · 브라우저) + 재현 절차 함께 올려주세요.
- 본인 환경에 한정된 문제는 `docker compose logs --since=10m backend` 첨부가 가장 도움 됩니다.
