# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-28 (phase 09 MCP server + 라이브 검증 통과 — M5 절반)
- 직전 작업(phase 09 mcp-server):
  - `spring-ai-bom:1.0.0` + `spring-ai-starter-mcp-server-webmvc` 도입. application.yml에 `spring.ai.mcp.server.*`.
  - `MnemeTools`: 11개 `mn_*` 도구 — `mn_schema/mn_whoami/mn_list/mn_read/mn_search/mn_write/mn_update/mn_archive/mn_restore/mn_relations/mn_surface`. 외부 ID는 모두 base32 prefix.
  - `McpToolsConfig`: `MethodToolCallbackProvider` 빈 등록.
  - **`McpContextPropagationConfig`**: Reactor 비동기 파이프라인에서 SecurityContextHolder가 손실되는 문제를 해결 — Micrometer Context Propagation API에 SecurityContext `ThreadLocalAccessor` 등록 + `Hooks.enableAutomaticContextPropagation()`. boundedElastic 워커가 도구를 실행해도 원 서블릿 스레드의 인증이 그대로 보임.
  - `McpToolsIsolationTest` 6건 통과(read/archive/restore/relations 404, whoami 본인만).
  - 라이브 검증(2026-06-28): `/sse` GET → `data: /mcp/message?sessionId=...` endpoint event → `tools/list` 11개 inputSchema 정상 → `mn_whoami`가 `sdsd1008@gmail.com` 반환 + `mn_list`가 활성 폴더/메모리 노출 + `mn_search "코틀린"` → 코틀린 메모 score 0.367로 1위 ✅
- 이전 작업(phase 08 security-controls):
  - `RateLimitService` Caffeine 분/일/쓰기 분리 카운터 + `RateLimitFilter`(@Order 50) — userId 또는 IP 기반. 초과 시 429 ProblemDetail.
  - `TokenQuotaGuard` (@Transactional readOnly): `usage_daily` 조회 → 임베딩/채팅 호출 직전 한도 검사. 초과는 `OpenAiException.RateLimited`로 변환되어 메모리 저장은 계속(임베딩만 skip).
  - `PiiMaskingConverter` logback ClassicConverter — `mn_*`, `sk-*`, 이메일을 마스킹. `logback-spring.xml`에 `%maskedMsg` 패턴 토큰 등록.
  - `IsolationRegressionTest` 확장: api_key revoke/list, memory archive 케이스 추가. REST 전수 회귀는 Testcontainers 호환 이슈로 phase 15(client-validation)에 이관.
  - 라이브 검증(2026-06-28):
    - 쓰기 21번째 요청 → 429 차단 ✅
    - embed_tokens 100k 강제 후 메모리 작성 → `has_embed=false` + 본문 저장 정상 ✅
  - **M4 마일스톤(보안 완전) 도달**
- 이전 작업(phase 07 hybrid-search):
- 직전 작업(phase 07 hybrid-search):
  - `SearchService` 단일 네이티브 SQL — α·(1−cosine) + β·ts_rank + γ·similarity.
  - `SearchProperties` 가중치(α=0.6/β=0.3/γ=0.1) `mneme.search.*` 환경변수 노출.
  - `SearchController /api/search?q=&folderExtId=&tags=&from=&to=&limit=`. 본문은 응답에서 제외(상세는 `/api/memories/{id}`).
  - JPA 네이티브 결과 timestamp는 Instant로 매핑되는 점 보정 — `toOffsetDateTime` 헬퍼.
  - 라이브 검증(2026-06-28): 메모리 4건 작성 후 "벡터 검색" → pgvector 메모리 1위, "프론트엔드 라이브러리" → React 18 1위로 의미 기반 정렬 정상 ✅
- 이전 작업(phase 06 llm-adapter):
- 직전 작업(phase 06 llm-adapter):
  - OpenAI REST API 직접 호출 어댑터(ADR-020). Spring AI는 phase 09(MCP) 한정.
  - `LlmProperties`(`mneme.openai.*`) + `OpenAiClient`(RestClient + 401/429/4xx/5xx 분리) + `OpenAiException`
  - `EmbeddingService`: text-embedding-3-small 1536d + SHA-256 키 Caffeine 캐시(5000/30min). 캐시 히트 시 OpenAI 미호출.
  - `PromptGuard.fence`: 8KB 절단 + `<<<USER_CONTENT … END_USER_CONTENT>>>` 펜스 + 마커 이스케이프(프롬프트 인젝션 1차 방어).
  - `ChatService`: summarize/classifyFolder/suggestTags. 시스템 프롬프트 리소스 파일 분리. 실패는 swallow.
  - `MemoryWriteFacade`(non-`@Transactional`): 외부 호출 → MemoryService.create/update(tx1) → MemoryEmbeddingDao 네이티브 UPDATE(tx2). CLAUDE.md "외부 호출은 트랜잭션 밖" 규칙 준수.
  - `PgUsageRecorder` REQUIRES_NEW: `usage_daily` INSERT … ON CONFLICT 일일 토큰 집계. NoOp 빈은 `@ConditionalOnMissingBean`으로 비활성화.
  - SecurityConfig 보정: Authorization Bearer 헤더 보유 요청은 CSRF 면제(phase 04 후속 보안 보정).
  - 라이브 검증(2026-06-28): 폴더+메모리 생성 → `embedding` 1536d 정상 + `summary` 자동 생성("코틀린은 JVM에서 동작하며…") + `usage_daily(embed_tokens=73, llm_in=144, llm_out=22)` 정상 ✅
- 이전 작업(phase 03 라이브 OAuth): sdsd1008@gmail.com 정상 로그인. version 컬럼·kotlin-jpa 플러그인·예외 매핑 3건 보정 commit ed0faa2.
- **사용자 액션 필요**:
  - 없음(phase 07 hybrid-search는 자동 진행 가능, 라이브 검증도 OpenAI 키 재사용)
- 작성자 노트: phase 07(hybrid-search)은 pgvector + tsvector + pg_trgm 결합. 자동 진행 후 라이브 검증.

## 한 줄 컨텍스트

Mneme: 여러 AI 클라이언트(Claude/ChatGPT/Codex/Grok)가 공유하는 영구 메모리 인프라. Heirmos 클론 + 위키적 확장. Kotlin + Spring Boot + Postgres+pgvector + React. 셀프호스팅 가능, ~10명 규모 비상업.

## 새 세션 즉시 진입 가이드

1. 작업 디렉토리: `/Users/lyw/Documents/self/workspace/mneme`
2. 반드시 읽기:
   - `docs/HANDOFF.md`
   - `docs/ROADMAP.md`
   - `docs/PRD.md`
   - `docs/ARCHITECTURE.md`
   - `docs/ADR.md`
   - `docs/DEVELOPMENT.md` (로컬 개발 환경 + env 변수)
   - `docs/SECURITY.md` (보안 정책)
   - 현재 구현 phase의 `phases/{phase}/index.json`과 `stepN.md`
3. 현재 작업 위치: phase 00 docs-bootstrap 완료, 사용자 리뷰 게이트 → 01 project-skeleton 시작 전 (phase 디렉토리 아직 미생성)
4. 작업 종료 시:
   - 해당 step에 `"status": "completed"`와 `"summary"` 기록
   - 본 파일의 "마지막 업데이트"와 "Phase 진행 상태" 갱신
   - `CHANGELOG.md`에 마일스톤 도달 시 항목 추가
   - conventional commit 작성

## 핵심 의사결정 (요약)

- 백엔드: Kotlin + Spring Boot 3.x (패키지 `com.mneme`, JDK 21)
- DB: PostgreSQL 16 + pgvector + pg_trgm 단일 DB
- LLM: OpenAI 단일 (text-embedding-3-small + gpt-4o-mini), Spring AI 추상화
- 인증: Google OAuth + `mn_` API 키(sha256), MCP는 OAuth DCR
- MCP: Spring AI MCP Server starter, 11개 도구 `mn_*` prefix
- 프론트엔드: React + Vite + shadcn/ui + TanStack Query + react-i18next
- 개발/배포: Docker Compose
- ID: UUID v7 + base32 외부 노출(prefix `mem_`/`fld_`/...)
- 폴더 1:N, 태그 N:M
- 마이그레이션: Flyway forward-only, 3단계 컬럼 변경
- 동시 편집: 낙관적 락 + 충돌 시 사용자 병합
- 보안 5종: 데이터 격리(테스트로 강제) / rate limit / 토큰 사용량 한도 / 보안 헤더 / PII 로그 마스킹
- 포터빌리티: export + import(Mneme zip / 일반 마크다운 zip)
- 시간: UTC 저장, 사용자 로컬 표시
- CI: GitHub Actions, 배포는 manual approve

자세한 결정 근거는 `docs/ADR.md` (ADR-001 ~ ADR-019).

## Phase 진행 상태

| Phase | 상태 | 메모 |
|-------|------|------|
| 00 docs-bootstrap | completed | 전체 문서 본문 + 보안·사용성·도메인 모델 + 추가 문서(DEVELOPMENT/SECURITY/CONTRIBUTING/LICENSE/CHANGELOG/.env.example) |
| 00.5 docs-llm-wiki-pivot | completed | Heirmos UI 검토 + LLM Wiki 패턴 채택. 본문 [[link]] MVP 핵심 승격, /map·/archive·키 빌더 추가. ADR-018/019 신규, ADR-003 개정 |
| 01 project-skeleton | completed | compose 4서비스 헬스 통과 + CI 통과. M1 마일스톤 달성 |
| 02 persistence-base | completed | Flyway V1(13 테이블+pgvector+pg_trgm), id 모듈(UUID v7+base32), 엔티티 7개, 리포지토리 7개. Testcontainers는 호환 이슈로 phase 05/08 보류 |
| 03 auth-google-oauth | completed (code + live ✅) | Spring Security + Google OAuth2 + 세션/CSRF/CORS/보안 헤더 + i18n 에러. 2026-06-28 라이브 검증 통과: sdsd1008@gmail.com → users row 정상 upsert. M2 마일스톤 도달 |
| 04 api-keys | completed | mn_ 키 발급/폐기/회전/이름수정 + Bearer 인증 필터 + AuditPublisher(key.created/revoked) |
| 05 memory-domain | completed | folder/memory/tag REST CRUD + 낙관적 락 + archive/restore + IsolationRegressionTest 베이스 |
| 06 llm-adapter | completed (code + live ✅) | OpenAI REST 어댑터 + Caffeine + PromptGuard + MemoryWriteFacade. 2026-06-28 라이브: embedding 1536d 저장 + 요약 + usage_daily 집계 정상 |
| 07 hybrid-search | completed (code + live ✅) | pgvector + tsvector + pg_trgm 결합 점수 + /api/search REST. 2026-06-28 라이브: 벡터/텍스트 의미 기반 정렬 정상 |
| 08 security-controls | completed (code + live ✅) | rate limit + token quota guard + PII 로그 마스킹 + 격리 회귀 확장. **M4 달성** |
| 09 mcp-server | completed (code + live ✅) | Spring AI MCP server starter(WebMVC) + 11개 mn_* @Tool + Reactor SecurityContext 자동 전파 + 도구 격리 테스트. 라이브에서 tools/list 11개, mn_whoami/mn_list/mn_search 정상 |
| 10~15 | pending | ROADMAP 참조 |
| 16 wiki-link-parser | pending | 본문 [[link]] 파서 + memory_links 동기 인덱스 + 제목 변경 시 backlink 본문 치환 |
| 17 memory-map-ui | pending | /map 그래프 페이지 + 편집기 [[ 자동완성 + backlink 패널 |
| 21~23 | deferred | Wiki 확장 (lint, folder index, review feedback) |
| 30~38 | deferred | 운영·배포·확장 |

## 외부 의존성 / 키 상태

| 항목 | 상태 | 메모 |
|------|------|------|
| OpenAI API 키 | 미발급 | text-embedding-3-small + gpt-4o-mini |
| Google OAuth 클라이언트 | 미생성 | GCP Console → OAuth consent → Web client + redirect URI |
| 도메인 | 미정 | `mneme.app`/`mneme.dev` 등 후보 |
| 호스팅 | 미정 | 로컬 docker compose 우선 |
| 백업 객체 스토리지 | 미정 | Backblaze B2 또는 S3 호환 |
| 이메일 발송 제공자 | 미정 | SendGrid/SES/Resend 중 |

## 다음 세션이 즉시 이어갈 작업

### 우선순위 1 (사용자): phase 03 라이브 검증

GCP Console에서 OAuth Client 발급 → `.env`에 입력 → `docker compose up` 재기동 → `/oauth2/authorization/google` 호출.

### 우선순위 2 (코드): phase 04 api-keys

1. `phases/04-api-keys/index.json`과 stepN.md 작성
2. `mn_<32B base62>` 평문 키 발급기 + sha256 해시 + 앞 8자 prefix
3. `/api/keys` REST 컨트롤러: 발급/목록/폐기/이름 수정. 발급 응답에만 평문 1회 노출
4. API 키 인증 필터: `Authorization: Bearer mn_...` 헤더 → prefix 조회 → keyHash 일치 → SecurityContext 주입
5. 감사 이벤트 발행(key.created, key.revoked)
6. WebMvc 테스트 + AuthN/AuthZ 케이스

phase 04는 사용자 개입 없이 진행 가능. 본격 라이브 통합은 phase 15(client-validation)에서.
