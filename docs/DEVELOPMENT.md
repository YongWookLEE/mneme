# Development Guide

로컬에서 Mneme를 개발하고 실행하기 위한 가이드.

## 사전 요구사항

- Docker 26+ (Docker Compose v2)
- Git
- JDK 21 (선택: 백엔드 IDE 개발 시)
- Node.js 20+ (선택: 프론트엔드 IDE 개발 시)
- OpenAI API 키 ([플랫폼 가입](https://platform.openai.com/))
- Google OAuth 클라이언트 (아래 발급 단계 참고)
- 발신용 이메일 제공자 키 (선택, MVP에선 `none`으로 두면 로그만 남김)

## OAuth 클라이언트 발급 (Google)

1. <https://console.cloud.google.com/> → 프로젝트 생성 또는 선택
2. APIs & Services → OAuth consent screen
   - User Type: External (테스트 단계)
   - Application name: `Mneme (local)`
   - Test users: 본인 계정 추가
3. APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: Web application
   - Name: `Mneme local`
   - Authorized JavaScript origins: `http://localhost:5173`, `http://localhost:8080`
   - Authorized redirect URIs: `http://localhost:8080/oauth/google/callback`
4. 발급된 Client ID / Client Secret을 `.env`에 입력

운영 도메인을 정한 후에는 같은 단계로 production 클라이언트를 별도 발급.

## OpenAI API 키 발급

1. <https://platform.openai.com/api-keys> → Create new secret key
2. 사용 한도 설정(Usage limits)에서 월 예산 상한 권장
3. 키를 `.env`의 `MNEME_OPENAI_API_KEY`에 입력

## 환경 변수

전체 목록은 [`docs/ARCHITECTURE.md` "환경 변수 명세"](ARCHITECTURE.md#환경-변수-명세) 또는 [`deploy/.env.example`](../deploy/.env.example) 참고.

가장 자주 건드리는 변수:

```bash
MNEME_BASE_URL=http://localhost:8080
MNEME_FRONTEND_ORIGIN=http://localhost:5173
MNEME_DB_URL=jdbc:postgresql://postgres:5432/mneme
MNEME_DB_USER=mneme
MNEME_DB_PASSWORD=change-me-in-local
MNEME_GOOGLE_OAUTH_CLIENT_ID=...
MNEME_GOOGLE_OAUTH_CLIENT_SECRET=...
MNEME_OPENAI_API_KEY=sk-...
SPRING_PROFILES_ACTIVE=local
```

`.env`는 `.gitignore`에 등록되어 있다. 절대 커밋하지 말 것.

## 로컬 실행

### 전체 스택 (권장)

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
docker compose -f deploy/docker-compose.yml logs -f backend
```

- 백엔드: <http://localhost:8080>
- 프론트엔드(Vite dev): <http://localhost:5173>
- Postgres: `localhost:5432`

### 백엔드만 IDE에서

```bash
docker compose -f deploy/docker-compose.yml up -d postgres
./gradlew :backend:bootRun
```

IntelliJ IDEA: `backend/build.gradle.kts` import, Run Configurations에 환경 변수 주입.

### 프론트엔드만

```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

백엔드 API origin은 `vite.config.ts`의 proxy로 `http://localhost:8080` 가리키도록 설정.

## 자주 쓰는 명령

| 명령 | 설명 |
|---|---|
| `docker compose ... up -d` | 전체 스택 백그라운드 기동 |
| `docker compose ... down` | 컨테이너 종료 (볼륨은 유지) |
| `docker compose ... down -v` | 컨테이너 + 볼륨 삭제 (DB 초기화) |
| `docker compose ... logs -f backend` | 백엔드 로그 추적 |
| `docker compose ... exec postgres psql -U mneme -d mneme` | DB 셸 |
| `./gradlew :backend:test` | 백엔드 단위 + Testcontainers 통합 |
| `./gradlew :backend:test --tests "*IsolationRegressionTest"` | 격리 회귀만 |
| `./gradlew :backend:ktlintFormat` | 포맷 자동 수정 |
| `./gradlew :backend:flywayMigrate` | 마이그레이션 수동 실행 |
| `npm --prefix frontend run test` | 프론트 단위 |
| `npm --prefix frontend run e2e` | Playwright E2E (백엔드 가동 필요) |
| `npm --prefix frontend run lint:fix` | 프론트 포맷 수정 |

## 시드 데이터 / 첫 키 발급

로컬에서 OAuth 없이 빠르게 시작하려면 Google 로그인을 한 번 한 다음 DB에 직접 키를 삽입한다.
[`docs/SELFHOST.md` §4-A](SELFHOST.md)에 상세 절차가 있다.

```bash
# 1) 브라우저에서 한 번 로그인
open http://localhost:8080/oauth2/authorization/google

# 2) user_id 확인 + 키 발급 (sha256 해시 직접 삽입)
docker compose -f deploy/docker-compose.yml exec postgres \
  psql -U mneme -d mneme -c "SELECT id, email FROM users;"

PLAIN="mn_$(openssl rand -hex 24)"
HASH=$(echo -n "$PLAIN" | shasum -a 256 | cut -d' ' -f1)
docker compose -f deploy/docker-compose.yml exec -T postgres \
  psql -U mneme -d mneme -c \
  "INSERT INTO api_keys (id, user_id, name, key_hash, prefix, created_at) \
   VALUES (gen_random_uuid(), '<YOUR_USER_ID>', 'dev', decode('$HASH','hex'), '${PLAIN:0:8}', now());"
echo "키: $PLAIN"
```

## DB 마이그레이션

현재 적용된 버전(2026-06-28):

| 버전 | 설명 |
|---|---|
| V1 | 13개 테이블 + pgvector/pg_trgm 확장 + ivfflat 임베딩 인덱스 + tsv 트리거 |
| V2 | `oauth_clients.user_id` NULL 허용(DCR 익명 등록) |
| V3 | `audit_events.ip` INET→TEXT |
| V4 | `folder_indexes` 테이블 (phase 21) |
| V5 | `memory_feedback` 테이블 + 인덱스 (phase 23) |

새 마이그레이션 추가:

```bash
ls backend/src/main/resources/db/migration/
$EDITOR backend/src/main/resources/db/migration/V6__your_change.sql
./gradlew :backend:flywayMigrate
```

규칙은 [`docs/ARCHITECTURE.md` 마이그레이션 정책](ARCHITECTURE.md#마이그레이션-정책-flyway) 참고. forward-only, 3단계 컬럼 변경, idempotent.

## 테스트 작성 규칙

- 단위 테스트: `backend/src/test/kotlin/com/mneme/unit/`
- 통합 테스트(Testcontainers): `backend/src/test/kotlin/com/mneme/integration/`
- **격리 회귀**: `backend/src/test/kotlin/com/mneme/security/IsolationRegressionTest.kt` — 신규 엔드포인트마다 케이스 추가
- 명명: `should_<expected>_when_<context>` 또는 한국어 백틱 ``` `사용자 A가 B의 메모리를 조회하면 404를 반환한다` ```

## 디버깅 팁

- 백엔드 디버그 포트: `JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
- Spring AI 호출 로그: `logging.level.org.springframework.ai=DEBUG` (단, 프롬프트에 사용자 입력 포함되니 운영에선 INFO)
- SQL 추적: `spring.jpa.properties.hibernate.format_sql=true` + `logging.level.org.hibernate.SQL=DEBUG`
- Postgres EXPLAIN: `EXPLAIN (ANALYZE, BUFFERS) ...`
- 임베딩 캐시 확인: `/actuator/caches`

## 자주 마주치는 문제

- **Flyway checksum mismatch**: 이미 적용된 마이그레이션 파일을 수정한 경우. 절대 수정 금지, 새 버전으로 보정.
- **pgvector 확장 없음**: `CREATE EXTENSION vector;`가 V1 마이그레이션에 포함됐는지 확인.
- **OAuth redirect mismatch**: `.env`의 `MNEME_BASE_URL`과 Google Console에 등록한 redirect URI가 정확히 일치해야 함(슬래시/포트 포함).
- **Docker 컨테이너에서 host 접근 안됨**: macOS/Linux는 `host.docker.internal` 사용, Linux 일부 배포판에선 `--add-host=host.docker.internal:host-gateway` 필요.

## 새 phase 시작하기

1. `docs/HANDOFF.md` 읽기 → 다음 phase 확인
2. `phases/<phase-name>/` 디렉토리 생성
3. `phases/_template/` 참고해 `index.json` + `stepN.md` 작성
4. `step1.md`부터 순차 실행
5. step 완료 시 `index.json`의 step에 `"status": "completed"` + `"summary"` 기록
6. phase 완료 시 `docs/HANDOFF.md` 갱신, 마일스톤 도달 시 `CHANGELOG.md` 갱신
