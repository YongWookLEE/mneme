# Architecture

## 개요

Mneme는 백엔드 단일 프로세스(Spring Boot)와 SPA 프론트엔드(React)로 구성된 멀티테넌트 메모리 서비스다. 백엔드는 HTTP API, MCP 서버, OAuth 인증, LLM 호출, DB 접근을 모두 담당한다. 데이터는 PostgreSQL(+pgvector) 단일 인스턴스에 메타데이터·전문검색 인덱스·벡터를 함께 저장한다. 모든 컴포넌트는 Docker Compose로 통합되어 로컬·운영 환경에서 동일하게 동작한다.

## 디렉터리 구조

```text
.
├── backend/                            # Kotlin + Spring Boot 3
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/mneme/
│       │   │   ├── api/                # REST 컨트롤러 (대시보드용, 세션 인증)
│       │   │   ├── mcp/                # MCP 도구 정의 (@Tool, Bearer 인증)
│       │   │   ├── auth/               # Google OAuth + API 키 + DCR
│       │   │   ├── memory/             # Memory/Folder/Tag 도메인
│       │   │   ├── llm/                # OpenAI 어댑터 (임베딩/요약/분류)
│       │   │   ├── search/             # 하이브리드 검색 (벡터 + tsvector + trgm)
│       │   │   ├── export/             # 데이터 export/import
│       │   │   ├── security/           # rate limit, CORS, 헤더, PII 마스킹
│       │   │   ├── observability/      # 메트릭, 감사 로그, 사용량 추적
│       │   │   ├── notification/       # 이메일 발송 (SendGrid/SES 어댑터)
│       │   │   ├── persistence/        # JPA 엔티티, 리포지토리, Flyway
│       │   │   ├── id/                 # UUID v7 생성, base32 인코딩
│       │   │   └── config/             # Spring 설정, 프로파일별 오버라이드
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-prod.yml
│       │       ├── db/migration/       # Flyway V1__init.sql ...
│       │       └── i18n/messages_ko.properties
│       └── test/
│           ├── kotlin/com/mneme/
│           │   ├── unit/
│           │   ├── integration/        # Testcontainers
│           │   └── security/           # 격리 회귀 테스트 (CRITICAL)
│           └── resources/
├── frontend/                           # React + Vite + shadcn/ui
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── pages/                      # Dashboard, ApiKeys, Guide, Settings, Onboarding, Login
│       ├── components/                 # FolderTree, MarkdownView, MarkdownEditor, SearchBar, EmptyState, ConfirmDialog
│       ├── lib/
│       │   ├── api.ts                  # fetch wrapper, 401 핸들링
│       │   ├── auth.ts                 # 세션 훅
│       │   ├── queries.ts              # TanStack Query 정의
│       │   ├── shortcuts.ts            # 키보드 단축키 매핑
│       │   └── i18n.ts                 # react-i18next 설정
│       ├── styles/
│       └── locales/ko/                 # 한국어 번역
├── deploy/
│   ├── docker-compose.yml              # 운영용 (백엔드 jar + Postgres + Caddy)
│   ├── docker-compose.override.yml     # 로컬 전용 (hot reload, dev seed)
│   ├── Caddyfile                       # 자동 HTTPS, 리버스 프록시
│   ├── .env.example                    # 모든 환경변수 + 기본값 + 설명
│   ├── scripts/
│   │   ├── backup.sh                   # pg_dump → 외부 스토리지
│   │   ├── restore.sh                  # pg_restore from snapshot
│   │   └── seed-dev.sh                 # 로컬 개발용 시드
│   └── README.md                       # 셀프호스팅 가이드 (스크린샷 포함)
├── docs/                               # PRD, ARCHITECTURE, ADR, ROADMAP, HANDOFF, UI_GUIDE, DEVELOPMENT, SECURITY, CONTRIBUTING
├── phases/                             # 단계별 실행 지시 (Basic Harness)
├── scripts/                            # Basic Harness 실행기 (수정 금지)
├── .github/workflows/                  # CI (lint + test + build)
├── README.md                           # 프로젝트 소개 + 빠른 시작
├── LICENSE                             # MIT
├── CHANGELOG.md                        # 변경 로그
├── CLAUDE.md                           # Claude용 프로젝트 헌법
└── AGENTS.md                           # Codex용 프로젝트 헌법
```

## 모듈 경계

- `auth`: Google OAuth 콜백, 세션 발급, API 키 발급·해시·검증·회전, DCR 엔드포인트, 감사 이벤트 발행
- `memory`: Memory/Folder/Tag/MemoryLink 엔티티 관리. 본문 `[[wiki-link]]` 파서 포함(메모리 저장/수정 트랜잭션 내 동기 호출). **모든 메서드 user_id 첫 인자 강제**
- `llm`: Spring AI OpenAI 어댑터. 시스템 프롬프트 상수, 사용자 입력은 인용 블록으로 감쌈
- `search`: 하이브리드 검색. SQL은 prepared statement, user_id 조건 누락 거부
- `mcp`: 11개 `mn_*` 도구를 `@Tool`로 등록. Streamable HTTP. Bearer 인증
- `api`: 대시보드 REST. 세션 쿠키 + CSRF 토큰
- `export`: zip 생성/파싱. 마크다운 파일 + manifest.json 형식
- `security`: rate limit, CORS, 보안 헤더, 응답 직렬화 마스킹
- `observability`: Micrometer 메트릭, 감사 로그, 사용자별 토큰 사용량 집계
- `notification`: 이메일 어댑터 (SendGrid/SES). 임계치 알림, 계정 삭제 안내
- `id`: UUID v7 생성, base32 인코딩(소문자, 26자, prefix `mem_`/`fld_` 등)
- `persistence`: JPA + Flyway. pgvector 컬럼은 native query

## 환경 변수 명세

`deploy/.env.example`로도 동일 제공.

| 변수 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `MNEME_BASE_URL` | ✅ | `http://localhost:8080` | 외부 노출 URL. OAuth redirect와 MCP 엔드포인트의 절대 URL 생성 |
| `MNEME_FRONTEND_ORIGIN` | ✅ | `http://localhost:5173` | CORS 허용 origin |
| `MNEME_DB_URL` | ✅ | `jdbc:postgresql://postgres:5432/mneme` | JDBC URL |
| `MNEME_DB_USER` | ✅ | `mneme` | DB 사용자 |
| `MNEME_DB_PASSWORD` | ✅ | (없음) | DB 비밀번호 |
| `MNEME_GOOGLE_OAUTH_CLIENT_ID` | ✅ | (없음) | Google Cloud Console에서 발급 |
| `MNEME_GOOGLE_OAUTH_CLIENT_SECRET` | ✅ | (없음) | Google Cloud Console에서 발급 |
| `MNEME_OPENAI_API_KEY` | ✅ | (없음) | OpenAI 플랫폼에서 발급 |
| `MNEME_OPENAI_EMBED_MODEL` | ❌ | `text-embedding-3-small` | 변경 시 model_version 백필 잡 필요 |
| `MNEME_OPENAI_LLM_MODEL` | ❌ | `gpt-4o-mini` | 분류/요약용 |
| `MNEME_RATE_LIMIT_PER_MIN` | ❌ | `60` | 사용자별 분당 호출 상한 |
| `MNEME_RATE_LIMIT_PER_DAY` | ❌ | `5000` | 사용자별 일일 호출 상한 |
| `MNEME_RATE_LIMIT_WRITE_PER_MIN` | ❌ | `20` | write 도구 분당 상한 |
| `MNEME_TOKEN_LIMIT_EMBED_PER_DAY` | ❌ | `100000` | 일일 임베딩 토큰 한도 |
| `MNEME_TOKEN_LIMIT_LLM_PER_DAY` | ❌ | `50000` | 일일 LLM 입출력 토큰 한도 |
| `MNEME_SESSION_COOKIE_DOMAIN` | ❌ | (현재 도메인) | 쿠키 도메인 |
| `MNEME_SESSION_TTL_HOURS` | ❌ | `168` | 세션 유효 기간 (7일) |
| `MNEME_OAUTH_ACCESS_TOKEN_TTL_MIN` | ❌ | `30` | MCP access token 유효 분 |
| `MNEME_OAUTH_REFRESH_TOKEN_TTL_DAYS` | ❌ | `30` | refresh token 유효 일 |
| `MNEME_EMAIL_PROVIDER` | ❌ | `none` | `sendgrid` / `ses` / `none`(로그만) |
| `MNEME_EMAIL_API_KEY` | ❌ | (없음) | 이메일 발송 키 |
| `MNEME_EMAIL_FROM` | ❌ | `noreply@example.com` | 발신 주소 |
| `MNEME_ADMIN_EMAILS` | ❌ | (없음) | 콤마 구분, 관리자 알림 수신 |
| `MNEME_BACKUP_S3_ENDPOINT` | ❌ | (없음) | B2/S3 호환 백업 대상 |
| `MNEME_BACKUP_S3_BUCKET` | ❌ | (없음) | |
| `MNEME_BACKUP_S3_ACCESS_KEY` | ❌ | (없음) | |
| `MNEME_BACKUP_S3_SECRET_KEY` | ❌ | (없음) | |
| `SPRING_PROFILES_ACTIVE` | ❌ | `prod` | 로컬은 `local` |
| `JAVA_OPTS` | ❌ | `-Xmx1g` | 힙 크기 조정 |

비밀 관리: 운영은 `.env` 파일 권한 `600`, 또는 Docker secrets / 호스팅 사업자 secrets manager. 절대 git 커밋 금지(`.gitignore` 등록).

## 서비스 시작 순서 (Docker Compose)

1. `postgres`: pgvector 이미지 부팅. healthcheck로 `pg_isready` 통과까지 대기
2. `backend`: Postgres 준비 후 시작. Flyway 마이그레이션 실행 → Spring 컨텍스트 부트 → `/actuator/health` 200
3. `frontend-dev` (로컬만): Vite dev 서버. 백엔드 health 무관하게 부트
4. `caddy`: 백엔드 health 통과 후 트래픽 라우팅 시작

backend의 OpenAI/Google 연결성은 lazy — 첫 요청 시 검증. 시작 시점에는 환경변수 존재만 확인.

## 데이터 흐름

### 메모리 저장 (write)

1. 클라이언트 → `mn_write` MCP 도구 또는 `POST /api/memories`
2. `security`: 요청 크기 검사(본문 ≤ 256KB), rate limit 카운터 +1
3. `auth`: Bearer 또는 세션으로 user_id 확인. 실패 시 401
4. `memory`: 입력 원문 + user_id로 도메인 서비스 호출. 트랜잭션 시작
5. `llm`: 분류·요약·임베딩·태그 추출 병렬 호출. 시스템 프롬프트와 사용자 입력 명확히 분리
6. `observability`: OpenAI 호출 토큰 수 집계, 사용자별 일일 한도 초과 시 호출 차단(429)
7. UUID v7 ID 생성 → INSERT `memories` (user_id, folder_id, ...). tsvector 트리거 자동 갱신
8. **본문 `[[wiki-link]]` 파싱** → `memory_links` 동기 갱신(트랜잭션 내). 기존 backlink 중 본문에서 사라진 것 삭제, 새로 추가된 것 INSERT, 제목 매칭 실패는 `target_id = NULL`(깨진 링크)로 기록
9. 트랜잭션 커밋
10. `auth`: 감사 이벤트(memory.created, links: N) 비동기 기록
11. 응답: `{id, path, title, summary, tags, links: { outgoing: N, broken: M }}`

### 메모리 제목 변경 시 backlink 일관성

- 제목 변경은 `mn_update`의 일부. 트랜잭션 안에서:
  1. `memories.title` UPDATE
  2. `memory_links WHERE target_id = :id` 조회 → 영향 받는 `source_id` 목록
  3. 각 source 본문에서 옛 `[[옛 제목]]`을 새 `[[새 제목]]`으로 치환(정확 매칭만, 다른 의미 텍스트 안 건드림)
  4. source 메모리들의 `memory_links.target_label` 갱신
  5. tsvector 재계산 트리거 (source 메모리들에 대해)
- 영향 메모리 수가 많으면(예: > 50) 응답에 `affected_count` 포함해 사용자 확인 UI 표시

### 검색 (search)

1. 클라이언트 → `mn_search` 또는 `POST /api/search`
2. `auth`로 user_id 확보 → 모든 후속 쿼리에 강제 주입
3. `llm`: 쿼리 임베딩 1회 (캐시: 같은 쿼리 5분)
4. `search`: 단일 SQL에서 점수 계산
   - 벡터: `1 - (embedding <=> :query_vec)`
   - 키워드: `ts_rank(tsv, plainto_tsquery(:query))`
   - 트라이그램(한국어 보강): `similarity(content, :query)`
   - 최종: `α * vector + β * keyword + γ * trgm`, α+β+γ=1, 기본 (0.6, 0.3, 0.1)
   - `WHERE user_id = :uid AND archived_at IS NULL AND <필터>`
5. 상위 N개 반환 (본문은 토큰 절약 위해 요약만, 본문은 `read`로 별도 조회)

### MCP 연결 (Claude.ai 등)

1. 사용자가 Claude.ai에서 Custom connector URL `https://mneme.example.com/mcp` 입력
2. 클라이언트 → `/oauth/register` (RFC 7591 DCR로 client_id 동적 발급) → `/oauth/authorize`
3. 사용자 Google 로그인 + 동의
4. 콜백 → 클라이언트가 `/oauth/token`으로 access_token 교환
5. MCP 요청 헤더 `Authorization: Bearer <token>`. 내부적으로 `oauth_<sub>` 가상 키로 user_id 매핑

### 데이터 export

1. 사용자 → 대시보드 "내 데이터 다운로드"
2. `export` 백그라운드 잡으로 zip 생성 (진행 상태는 폴링)
3. zip 구조:
   ```
   manifest.json                       # 사용자 메타, 메모리 ID 매핑, 폴더 트리, export 시각, 모델 버전
   folders/projects/mneme/decisions/
     2026-06-27_design-rationale.md    # frontmatter + 본문
   audit.log                           # 본인 감사 이벤트 (선택, 옵트인)
   ```
4. 완료 시 1회용 다운로드 링크 발급 (24h 유효, 다운로드 1회 후 무효)

### 데이터 import

1. 사용자 → 대시보드 "데이터 가져오기" → zip 업로드
2. `export` 모듈이 zip 파싱
3. Mneme zip(`manifest.json` 존재) → 메타·폴더·태그 보존, ID 충돌 시 사용자 선택(건너뜀/덮어쓰기/이름 변경)
4. 일반 마크다운 zip → 폴더 구조 그대로 보존, LLM 분류 건너뛰고 폴더 경로 사용, 임베딩은 자동 생성
5. 모든 import는 비동기 잡, 진행 상태 표시

## 데이터 모델

```sql
-- 사용자
users (
  id              UUID PRIMARY KEY,
  google_sub      TEXT UNIQUE NOT NULL,
  email           TEXT NOT NULL,
  locale          TEXT NOT NULL DEFAULT 'ko',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ                       -- 계정 삭제 유예 시작
)

-- API 키
api_keys (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  key_hash        BYTEA NOT NULL,                   -- sha256
  prefix          TEXT NOT NULL,                    -- 앞 8자 식별
  last_used_at    TIMESTAMPTZ,
  revoked_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)
INDEX api_keys_prefix_idx ON api_keys(prefix) WHERE revoked_at IS NULL;
INDEX api_keys_user_idx ON api_keys(user_id);

-- OAuth 클라이언트 (DCR)
oauth_clients (
  id                  UUID PRIMARY KEY,
  user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id           TEXT UNIQUE NOT NULL,
  client_secret_hash  BYTEA,                        -- public client는 null
  redirect_uris       TEXT[] NOT NULL,
  client_name         TEXT,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- OAuth 토큰
oauth_tokens (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id       TEXT NOT NULL,
  access_hash     BYTEA NOT NULL,
  refresh_hash    BYTEA,
  scope           TEXT,
  expires_at      TIMESTAMPTZ NOT NULL,
  refresh_expires_at TIMESTAMPTZ,
  revoked_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- 세션 (대시보드)
sessions (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  csrf_token      TEXT NOT NULL,
  expires_at      TIMESTAMPTZ NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- 폴더
folders (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  parent_id       UUID REFERENCES folders(id) ON DELETE RESTRICT,
  path            TEXT NOT NULL,                    -- 정규화 경로 /projects/mneme/
  name            TEXT NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, path)
)
INDEX folders_user_path_idx ON folders(user_id, path);
INDEX folders_user_parent_idx ON folders(user_id, parent_id);

-- 메모리
memories (
  id              UUID PRIMARY KEY,                 -- UUID v7
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  folder_id       UUID NOT NULL REFERENCES folders(id) ON DELETE RESTRICT,
  title           TEXT NOT NULL,
  content         TEXT NOT NULL,
  summary         TEXT,
  embedding       VECTOR(1536),
  tsv             TSVECTOR,
  source_uri      TEXT,
  byte_size       INT NOT NULL,
  model_version   TEXT NOT NULL DEFAULT 'text-embedding-3-small@1',
  archived_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)
INDEX memories_user_archived_idx ON memories(user_id, archived_at);
INDEX memories_user_folder_idx ON memories(user_id, folder_id);
INDEX memories_embedding_idx ON memories USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
INDEX memories_tsv_idx ON memories USING gin(tsv);
INDEX memories_content_trgm_idx ON memories USING gin(content gin_trgm_ops);
TRIGGER memories_tsv_update BEFORE INSERT OR UPDATE ON memories
  FOR EACH ROW EXECUTE FUNCTION tsvector_update_trigger(tsv, 'pg_catalog.simple', title, content);

-- 태그
tags (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  UNIQUE (user_id, name)
)
memory_tags (
  memory_id       UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
  tag_id          UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (memory_id, tag_id)
)

-- 메모리 링크 (본문 [[wiki-link]] 파싱 결과, MVP 핵심)
-- 본문이 진실, 이 테이블은 파생 인덱스. 본문 저장 시 트랜잭션 내에서 동기 갱신
memory_links (
  id              UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source_id       UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
  target_id       UUID REFERENCES memories(id) ON DELETE SET NULL,  -- NULL = 깨진 링크
  target_label    TEXT NOT NULL,                    -- 본문에 적힌 [[...]] 원문(제목 또는 mem_id)
  kind            TEXT NOT NULL DEFAULT 'wiki',     -- 'wiki' (본문 [[link]]). 'auto'는 후속 phase 예약
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)
INDEX memory_links_user_source_idx ON memory_links(user_id, source_id);
INDEX memory_links_user_target_idx ON memory_links(user_id, target_id);
INDEX memory_links_broken_idx ON memory_links(user_id) WHERE target_id IS NULL;

-- 메모리 버전 (수정 이력)
memory_versions (
  id              UUID PRIMARY KEY,
  memory_id       UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
  content         TEXT NOT NULL,
  edited_at       TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- 검토 (후속)
review_items (
  id              UUID PRIMARY KEY,
  memory_id       UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
  reason          TEXT NOT NULL,
  status          TEXT NOT NULL DEFAULT 'open'
)

-- 감사 로그
audit_events (
  id              UUID PRIMARY KEY,
  user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
  actor_kind      TEXT NOT NULL,                    -- 'user', 'api_key', 'oauth'
  action          TEXT NOT NULL,                    -- 'key.created', 'memory.archived', ...
  target_kind     TEXT,
  target_id       TEXT,
  ip              INET,
  user_agent      TEXT,
  metadata        JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
)
INDEX audit_events_user_idx ON audit_events(user_id, created_at DESC);

-- 사용량 집계
usage_daily (
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  date            DATE NOT NULL,
  embed_tokens    INT NOT NULL DEFAULT 0,
  llm_in_tokens   INT NOT NULL DEFAULT 0,
  llm_out_tokens  INT NOT NULL DEFAULT 0,
  request_count   INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, date)
)
```

전 테이블 user_id 인덱스 + 외래키 ON DELETE CASCADE(또는 RESTRICT, 폴더처럼 부모 보존이 의미 있는 곳).

## 마이그레이션 정책 (Flyway)

- 위치: `backend/src/main/resources/db/migration/`
- 파일명: `V{버전}__{설명}.sql` (예: `V1__init.sql`, `V2__add_audit_events.sql`)
- **forward-only**: rollback 마이그레이션 만들지 않음. 잘못된 마이그레이션은 새 버전으로 보정
- 모든 마이그레이션은 트랜잭션 안에서 실행, 실패 시 롤백
- 컬럼 추가는 nullable로 시작 → 백필 → NOT NULL 변경 (3단계)
- 인덱스 추가는 `CREATE INDEX CONCURRENTLY` (DDL 트랜잭션 밖에서 별도 실행, Flyway `@Repeatable` 또는 별도 잡)
- 마이그레이션은 idempotent하게 작성 (`IF NOT EXISTS` 등)

## 트랜잭션 경계

- 컨트롤러는 트랜잭션 없음 (`@Transactional` 금지)
- 서비스 레이어에서 `@Transactional`
- 읽기 전용은 `@Transactional(readOnly = true)`
- 외부 호출(OpenAI/이메일)은 트랜잭션 **밖**에서 — DB 트랜잭션을 외부 호출 동안 잡지 않음
- 패턴: ① 외부 호출 결과 수집 → ② 트랜잭션 내 INSERT/UPDATE → ③ 트랜잭션 후 비동기 이벤트(감사, 알림)

## 동시성

- 메모리 수정: 낙관적 락. JPA `@Version` 컬럼(`updated_at`을 version으로 사용 또는 별도 `version BIGINT`)
- 충돌 시 409 + 두 버전 본문 응답 → 프론트엔드에서 diff 표시
- 폴더 이동: 자식 메모리 path 일괄 갱신은 단일 트랜잭션
- 토큰 사용량 카운터: `INSERT ... ON CONFLICT (user_id, date) DO UPDATE SET ... += ...` (원자적)
- Rate limit 카운터: in-memory token bucket. 정확도 1초 단위, 한도 도달 시 429

## 캐싱

- 쿼리 임베딩: Caffeine 캐시 5분, 키는 쿼리 문자열 + user_id (다른 사용자 검색 단어 격리)
- `whoami` 응답: 사용자별 30초
- 폴더 트리: 사용자별 60초, 폴더 CRUD 시 무효화
- 캐시는 단일 인스턴스 가정. 수평 확장 시 Redis 전환

## 보안

### 인증·인가
- 세션 쿠키: `Secure`, `HttpOnly`, `SameSite=Lax`. CSRF 토큰 헤더로 검증
- API 키: 발급 직후 평문 1회만 응답, DB는 sha256 해시 + prefix 8자
- 키 회전: 새 키 발급 후 14일 grace period, 그 사이 두 키 모두 유효, 그 후 옛 키 자동 폐기
- OAuth access token 30분, refresh token 30일. 폐기 즉시 캐시 무효화

### 데이터 격리 (CRITICAL)
- 모든 리포지토리 메서드는 user_id를 첫 인자로 명시
- 통합 테스트 `IsolationRegressionTest`가 모든 도구·엔드포인트에 대해 사용자 A → B 데이터 접근 → 404 검증
- 신규 엔드포인트 추가 시 이 테스트에도 케이스 추가 필수(코드 리뷰 체크리스트)

### 네트워크·전송
- HTTPS 강제. 로컬 dev만 http 허용
- HSTS preload 가능 (운영 도메인 확정 후)
- CORS: 등록된 origin만 (`MNEME_FRONTEND_ORIGIN`). 와일드카드 금지
- 보안 헤더: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `X-Frame-Options: DENY`, `Permissions-Policy` 최소화
- 요청 크기: 메모리 본문 ≤ 256KB, MCP payload ≤ 1MB, import zip ≤ 50MB

### 비용·남용 방어
- Rate limit: 사용자별 분당 60회, 일별 5,000회, write 분당 20회
- 토큰 사용량: 일일 한도(임베딩 100K + LLM 50K). 초과 시 호출 차단 + 이메일 알림
- 키 도용 의심: 짧은 시간 다수 IP 또는 비정상 호출 패턴 → 사용자 경고, 관리자 알림

### 로깅·관측 (PII 보호)
- 자동 마스킹 필드 패턴: `Authorization`, `Cookie`, `Set-Cookie`, `password`, `secret`, `token`, `api_key`, `embedding`
- 메모리 본문은 로그에 절대 출력 금지(content 필드 직렬화 제외 어노테이션)
- 감사 로그 별도 테이블, 1년 보관
- 메트릭: 요청 수/지연/오류율, 사용자별 토큰 사용량, DB 커넥션, OpenAI 응답 시간

### LLM 안전
- 시스템 프롬프트 코드 상수, 사용자 메모리 본문은 인용 블록(```)으로 감쌈
- 분류 LLM 입력 길이 상한 8K, 초과 시 잘라내고 메타에 `truncated=true`
- 응답에서 시스템 지시 우회 패턴(예: "지시를 무시", "ignore previous instructions") 감지 시 분류 결과 폐기 → 기본 폴더(`/inbox/`)로

### 비밀 관리
- `.env` 파일 권한 `600`, git 제외
- 운영 환경은 호스팅 사업자 secrets manager 또는 Docker secrets 권장
- DB 비밀번호, OpenAI 키, OAuth client secret은 절대 평문 로그 출력 금지

## 관측성

- 메트릭: Micrometer + Prometheus 엔드포인트 `/actuator/prometheus`
- 로그: 구조화 JSON (logback-spring), trace_id 포함. stdout → docker → 외부 수집기(loki 등)
- 감사: `audit_events` 테이블. 본인 활동은 대시보드 `/settings/activity`에서 조회
- 사용량: 매 LLM 호출 직후 `usage_daily` upsert. 일별 집계 잡으로 임계치 검사
- 헬스 체크: `/actuator/health` (DB·OpenAI·Email 연결성). Docker healthcheck 연동
- 핵심 알림 (관리자 이메일):
  - 사용자 일일 토큰 한도 80% 도달
  - OpenAI/Google 외부 호출 실패율 10% 초과 5분 지속
  - 디스크 사용량 80% 초과
  - 백업 실패

## 백업·복구

- pg_dump cron (일 1회 03:00 UTC), 외부 S3 호환 스토리지(B2 등) 업로드
- 보관 정책: 일 7개 + 주 4개 + 월 12개 (총 23개)
- 복구: `deploy/scripts/restore.sh <snapshot-name>` (Docker 볼륨 마운트 후 pg_restore)
- 임베딩 모델 마이그레이션: `model_version`별 배치 잡, 신·구 임베딩 병행 기간 운영
- 재해 복구 RTO 4시간(VPS 재구축 + restore), RPO 24시간(일 백업)

## ID·인코딩

- 모든 엔티티 PK: UUID v7 (시간순 정렬, 인덱스 효율 좋음)
- 외부 노출 ID: prefix + base32(소문자) — `mem_2v8gqj4z7n3kh5p9r1t6w0xy2a`
- prefix: `mem_`(memory), `fld_`(folder), `tag_`(tag), `key_`(api_key)
- API/MCP는 외부 노출 ID만 받음. 내부 변환은 `id` 모듈에서

## 폴더 이동 시 부수효과

1. 사용자가 `/a/b/` → `/x/y/b/` 이동 요청
2. 트랜잭션 시작
3. 대상 경로 충돌 검사 (UNIQUE 위반 시 409)
4. 폴더 자체 path 갱신
5. 자식 폴더·메모리의 path를 SQL `UPDATE ... WHERE path LIKE '/a/b/%'`로 일괄 갱신
6. 감사 이벤트 (folder.moved, affected_count)
7. 커밋
8. 캐시 무효화 (폴더 트리, 영향받은 메모리 목록)

## 오류 처리

- 모든 API/MCP 응답은 `{ok, data?, error?{code, message, trace_id}}` 봉투
- MCP는 JSON-RPC 표준 위에 동일 의미 매핑
- 에러 코드는 영어 식별자(`ERR_RATE_LIMIT`, `ERR_QUOTA_EXCEEDED`, `ERR_NOT_FOUND`, `ERR_CONFLICT`, `ERR_VALIDATION`, `ERR_AUTH`, `ERR_FORBIDDEN`, `ERR_INTERNAL`)
- 메시지는 i18n 키 → 한국어 번역. 상세 원인은 trace_id로 로그 조회
- 외부 의존성 장애: backoff retry 3회 (200ms, 1s, 5s) → 503 + retry-after
- 권한 위반은 항상 404 (존재 여부 노출 방지), 인증 실패만 401

## HTTP 상태 코드 매핑

| 상황 | 코드 | error.code |
|---|---|---|
| 검증 실패 | 400 | ERR_VALIDATION |
| 미인증 | 401 | ERR_AUTH |
| 권한 위반 / 존재하지 않음 | 404 | ERR_NOT_FOUND |
| 낙관적 락 충돌 | 409 | ERR_CONFLICT |
| Rate limit | 429 | ERR_RATE_LIMIT |
| 토큰 한도 초과 | 429 | ERR_QUOTA_EXCEEDED |
| 본문 크기 초과 | 413 | ERR_PAYLOAD_TOO_LARGE |
| 내부 오류 | 500 | ERR_INTERNAL |
| 외부 의존성 장애 | 503 | ERR_UPSTREAM |

## HikariCP 설정

```yml
spring.datasource.hikari:
  maximum-pool-size: 10          # 10명 규모, write 트래픽 낮음
  minimum-idle: 2
  connection-timeout: 5000
  idle-timeout: 300000
  max-lifetime: 1200000
  leak-detection-threshold: 60000
```

## 테스트 전략

- 단위: 도메인 로직 (검색 점수, 경로 정규화, 키 해시, rate limit, base32 인코딩) → JUnit5 + Kotest
- 통합: Testcontainers (Postgres+pgvector) → 리포지토리·검색·격리 검증, OpenAI는 mock
- **격리 회귀 (CRITICAL)**: `IsolationRegressionTest` 클래스가 모든 11개 도구 + REST에 대해 "사용자 A 컨텍스트로 사용자 B의 ID 접근 → 404" 자동 검증. 신규 엔드포인트는 여기에 케이스 추가 의무
- MCP: Spring AI MCP 클라이언트로 round-trip
- E2E: Playwright로 로그인 → 키 발급 → 메모리 저장 → 검색 → archive → restore → export → import
- LLM은 단위/통합에서 mock, 별도 "live" 프로파일에서 실제 호출 검증 (수동 실행)
- 보안 스캔: GitHub Actions에 Trivy(이미지) + dependency-check(의존성) 통합

## CI/CD

- GitHub Actions
- PR: ktlintCheck + backend test + frontend lint + frontend test + E2E smoke
- main 푸시: 위 + Docker 이미지 빌드 + GHCR 푸시 + 운영 환경 manual approve 배포 (배포 자체는 후속 phase)
- Dependabot: 주 1회 의존성 PR

## 상태 관리

- 서버 stateless: 세션·토큰은 DB 조회 + Caffeine 캐시
- 단일 인스턴스 가정이지만 코드는 stateless 유지 (수평 확장 옵션 보존)
- 프론트엔드: TanStack Query로 서버 상태 캐시, 로컬 UI 상태는 컴포넌트 useState/Zustand(검색 필터 등 페이지 단위 공유 상태만)

## 금지 패턴

- 평문 API 키 저장. 이유: 유출 시 전 사용자 키 노출
- LLM 호출을 트랜잭션 안에서 동기 차단. 이유: DB 커넥션 점유 + 응답 지연
- SQL/프롬프트에 사용자 입력 직접 보간. 이유: 인젝션
- 메모리 영구 DELETE. 이유: soft delete + 복구 약속. `archived_at` 사용
- 리포지토리 메서드에서 user_id 인자 누락. 이유: 데이터 격리 위반 위험
- 로그/응답에 메모리 본문·키 평문·토큰 노출. 이유: PII 유출
- Flyway rollback 마이그레이션 작성. 이유: 운영 위험, 새 버전으로 보정이 원칙
- 단일 거대 컨트롤러/서비스(>500줄). 이유: 모듈 경계 무너짐
- 컨트롤러에서 `@Transactional` 사용. 이유: 트랜잭션 경계가 컨트롤러로 새면 외부 호출이 트랜잭션 안에 들어가 DB 락 점유
