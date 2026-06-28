# Mneme 셀프호스팅 가이드

이 문서는 자기 서버(VPS·홈서버·Raspberry Pi 등 — 2GB RAM·2 vCPU 권장) 위에 Mneme를 띄우려는 사람을 위한 단계별 안내입니다.

> 로컬 개발 환경 셋업은 [`docs/DEVELOPMENT.md`](DEVELOPMENT.md), 문제 해결은 [`docs/TROUBLESHOOTING.md`](TROUBLESHOOTING.md)를 참고하세요.

## 1. 사전 준비

### 1-1. 서버

- Linux(Ubuntu 22.04+, Debian 12+ 권장) · Docker 26+ · docker compose plugin
- 외부 노출 시 80/443 포트 개방 + 도메인이 서버 IP를 가리키도록 DNS A 레코드 설정
- 디스크: pgvector + 메모리 본문 + 백업으로 최소 20GB

### 1-2. Google OAuth 클라이언트 등록

1. <https://console.cloud.google.com/apis/credentials> 접속 → 프로젝트 선택(또는 새로 생성)
2. **OAuth 동의 화면** → User Type "External" → 앱 이름·이메일 입력 → "테스트 사용자"에 본인 이메일 추가
3. **사용자 인증 정보 만들기 → OAuth 클라이언트 ID**
   - 애플리케이션 유형: **웹 애플리케이션**
   - 승인된 리디렉션 URI:
     - 로컬: `http://localhost:8080/login/oauth2/code/google`
     - 운영: `https://your-domain.com/login/oauth2/code/google`
4. 생성된 **클라이언트 ID**와 **클라이언트 보안 비밀번호** 메모

### 1-3. OpenAI API 키

1. <https://platform.openai.com/api-keys>에서 새 키 발급 (이름: "mneme-prod")
2. 사용량 한도(Settings → Limits)를 사용자 수 × 하루 1만 토큰 정도로 설정해 두면 안전

## 2. 코드 받고 환경 변수 작성

```bash
git clone https://github.com/<your-fork>/mneme.git
cd mneme
cp deploy/.env.example deploy/.env
```

`deploy/.env`를 열고 최소한 다음을 채웁니다:

```env
# 외부 노출 URL (운영)
MNEME_BASE_URL=https://your-domain.com
MNEME_FRONTEND_ORIGIN=https://your-domain.com

# DB
MNEME_DB_PASSWORD=<강한_랜덤_문자열_32바이트>

# Google OAuth
MNEME_GOOGLE_OAUTH_CLIENT_ID=<step 1-2 결과>
MNEME_GOOGLE_OAUTH_CLIENT_SECRET=<step 1-2 결과>

# OpenAI
MNEME_OPENAI_API_KEY=sk-...

# Caddy 자동 HTTPS (Let's Encrypt)
MNEME_DOMAIN=your-domain.com
ACME_EMAIL=you@example.com

# 운영 한도
MNEME_TOKEN_LIMIT_EMBED_PER_DAY=200000
MNEME_TOKEN_LIMIT_LLM_PER_DAY=100000
MNEME_RATE_LIMIT_PER_MIN=120
```

> 운영에서는 절대 기본값 그대로 두지 마세요. 특히 `MNEME_DB_PASSWORD`는 강한 랜덤이어야 합니다.

## 3. 띄우기

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
docker compose -f deploy/docker-compose.yml logs -f backend
```

부팅 로그에 다음이 보이면 정상:

```
Started MnemeApplicationKt in ~3 seconds
Registered tools: 11
Tomcat started on port 8080
```

## 4. 첫 사용

1. 브라우저로 `https://your-domain.com` 접속
2. 처음에는 AuthGate가 API 키를 요구합니다. 두 가지 방법:
   - **(a) 임시 키 직접 발급(권장)**: 아래 4-A 참고
   - **(b) Google OAuth 로그인 후 키 발급**: `https://your-domain.com/oauth2/authorization/google`

### 4-A. 임시 키 직접 발급

처음에는 본인 Google 계정으로 한 번 로그인해야 `users` 테이블에 본인 row가 생성됩니다.

```bash
# 1) OAuth 로그인 한 번 (브라우저에서)
open https://your-domain.com/oauth2/authorization/google

# 2) 본인 user_id 확인
docker compose -f deploy/docker-compose.yml exec postgres \
  psql -U mneme -d mneme -c "SELECT id, email FROM users;"

# 3) 평문 키 + 해시 생성
PLAIN="mn_$(openssl rand -hex 24)"
PREFIX="${PLAIN:0:8}"
HASH=$(echo -n "$PLAIN" | shasum -a 256 | cut -d' ' -f1)

# 4) DB에 직접 삽입 (USER_ID는 step 2 결과)
docker compose -f deploy/docker-compose.yml exec -T postgres \
  psql -U mneme -d mneme -c \
  "INSERT INTO api_keys (id, user_id, name, key_hash, prefix, created_at) \
   VALUES (gen_random_uuid(), '<YOUR_USER_ID>', 'initial', decode('$HASH','hex'), '$PREFIX', now());"

echo "키: $PLAIN"
```

이 평문 키를 대시보드 AuthGate에 입력하면 들어갑니다. 이후로는 대시보드 `/keys` 페이지에서 정식 발급 가능.

## 5. MCP 클라이언트 연결

대시보드 `/connect` 페이지에 Claude Desktop · Claude Code · Codex CLI · ChatGPT Developer mode용 스니펫이 있습니다. 키를 발급한 뒤 그대로 복사해서 쓰세요.

요약:

```bash
# Codex CLI (API 키 방식)
export MNEME_API_KEY="mn_..."
codex mcp add mneme --url https://your-domain.com/sse --bearer-token-env-var MNEME_API_KEY

# Codex CLI (OAuth 방식 권장)
codex mcp add mneme --url https://your-domain.com/sse
codex mcp login mneme
```

## 6. 운영 점검

- **헬스**: `https://your-domain.com/actuator/health` → `{"status":"UP"}`
- **메트릭**: `https://your-domain.com/actuator/prometheus` (Prometheus scrape 가능)
- **사용량**: 대시보드 `/usage`
- **감사 이벤트**: 대시보드 `/audit`
- **로그**: `docker compose -f deploy/docker-compose.yml logs -f backend`

## 7. 백업

[`docs/BACKUP.md`](BACKUP.md) 참고. 요약: `scripts/backup.sh`를 cron으로 매일 실행 → 외부 객체 스토리지(B2/S3/MinIO)에 업로드.

## 8. 업데이트

```bash
cd mneme
git pull
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build backend frontend-dev
```

Flyway 마이그레이션은 forward-only로 부팅 시 자동 실행됩니다. 운영 DB에 직접 DDL을 치지 마세요.

## 9. 데이터 이전 / 복구

- **다른 인스턴스에서 가져오기**: 대시보드 `/data` → Import에서 zip 업로드
- **백업 복원**: [`docs/BACKUP.md`](BACKUP.md)의 restore 절차
- **계정 삭제**: phase 33 후속(현재는 DB에서 user row 삭제 시 CASCADE로 모든 데이터 제거)

## 10. 자주 막히는 곳

[`docs/TROUBLESHOOTING.md`](TROUBLESHOOTING.md)에 모아 두었습니다.
