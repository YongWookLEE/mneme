# Changelog

[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 형식, [Semantic Versioning](https://semver.org/spec/v2.0.0.html) 지향.

## [Unreleased]

### Added
- 사용 가이드 페이지 `/help` — Mneme란·핵심 개념·페이지 안내·MCP 도구 11종·wiki link 동작·메모리 저장 시 LLM 흐름·피드백 학습·데이터 포터빌리티·보안 정책·단축키·FAQ를 한곳에 정리. 좌측 anchor 목차로 점프. Shell 헤더에 "도움말" 탭 + OnboardingTour 1단계에서 링크
- ADR-021~025 추가:
  - ADR-021 MCP Reactor SecurityContext 전파(Micrometer ContextPropagation)
  - ADR-022 본문 [[link]] 동기 인덱스 + backlink rename 별도 빈
  - ADR-023 폴더 인덱스 LLM 합성 정적 스냅샷
  - ADR-024 lint 4룰(broken/orphan/stub/dup-title), 자동 수정 없음
  - ADR-025 피드백 시스템 프롬프트 자동 inject

### Changed
- README — 현재 상태(M1~M8 완료) 명시 + 도움말 페이지 안내 추가
- ARCHITECTURE — 디렉토리 트리에 `wiki/`·`oauth/` 모듈 추가 + 모듈 경계 갱신 + Flyway V1~V5 표
- PRD — MVP 범위 항목별 구현 상태 ✅/⚠/❌ 표시 + M8 Wiki 확장 항목 추가 + MVP 제외 사항을 deferred phase로 명시
- UI_GUIDE — 실제 라우트(/help, /lint, /data, /usage, /audit, /connect, /map)와 폴더 인덱스·피드백·backlink 패널 반영
- DEVELOPMENT — 시드 데이터 → SELFHOST §4-A 키 직접 발급 절차로 교체 + Flyway V1~V5 현재 적용 표 추가
- CONTRIBUTING — 셀프호스팅 사용자 안내에 SELFHOST/TROUBLESHOOTING/BACKUP 링크 추가

- Phase 21 folder-index (code + live ✅, M8 진행):
  - V4 folder_indexes 테이블 + FolderIndex 엔티티 + Repository
  - ChatService.synthesizeFolderIndex + llm/prompts/folder-index.md(주제별 그루핑 + `[[wiki-link]]` 자동 + 빈 곳 추측)
  - FolderIndexService(LLM 호출은 트랜잭션 밖, save는 안) + GET/POST `/api/folders/{ext}/index{/rebuild}`
  - 프론트 `FolderIndexPanel` + `MemoryListPage` 상단 마운트
  - 라이브: 3건 메모리 → 주제별(데이터베이스/프론트엔드/프로그래밍) 분류 + `[[wiki-link]]` 정상
- Phase 22 lint-tools (code + live ✅, M8 진행):
  - `LintService.runAll` 4룰: broken(target_id=null), orphan(어디에도 연결 X), stub(<120B), dup-title(같은 폴더 동일 제목)
  - `LintController` `GET /api/lint` → `LintReport(counts + issues)`
  - 프론트 `LintPage /lint` 카테고리 카드 + 이슈 리스트 + Shell '검토' 탭
  - 라이브: broken 1 / orphan 2 / stub 2 감지
- Phase 23 review-feedback (code + live ✅, **M8 달성**):
  - V5 memory_feedback 테이블 + 엔티티/Repository
  - `FeedbackService` targets={folder,summary,tags,index,general} × values={up,down}
  - **핵심**: `FeedbackHintBuilder`가 `ChatService.call`에 자동 주입 — 시스템 프롬프트 후미에 최근 8건(부정 우선 정렬) 피드백 자동 append. 다음 LLM 호출이 자동 반영
  - `FeedbackController` POST/GET `/api/memories/{ext}/feedback`
  - 프론트 `FeedbackBar`(target+note+👍👎+최근 5건) + `MemoryDetailPage` 하단 마운트
  - 라이브: up/down 2건 저장·조회 정상
- Phase 17 memory-map-ui (code + live ✅, **M6.5 달성**):
  - `GraphController` `GET /api/graph` — 활성 메모리 노드 + wiki-link 엣지 + 깨진 링크
  - `BacklinksController` `GET /api/memories/{extId}/backlinks`
  - 프론트 `react-force-graph-2d` + `/map` 페이지(다크 캔버스, 노드 byteSize 크기, 깨진 링크 사이드 패널, 노드 클릭 라우팅)
  - `BacklinkPanel` 컴포넌트 → `MemoryDetailPage` 하단 마운트
  - Shell '맵' 탭 + 라이브 2026-06-28: graph 4노드 + 깨진 링크 1건 정상 ✅
- Phase 16 wiki-link-parser (code + live ✅):
  - `WikiLinkParser` — `[[제목]]`/`[[mem_<base32>]]` 정규식 파싱, 펜스/인라인 코드 마스킹. 4건 unit 테스트
  - `WikiLinkIndexer` — source 단위 reindex(`@Transactional`), 깨진 링크는 target_id=null
  - `MemoryWriteFacade.create/update`가 reindex 호출 + 제목 변경 시 `BacklinkRenameService.rename`(별도 빈, AOP 정상 적용) 호출
  - `llm/prompts/wiki-link-guide.md` 시스템 프롬프트 가이드(코드 블록 무시 안내 포함)
  - 라이브 2026-06-28: `[[코틀린 vs 자바 v2]]` → `[[Kotlin vs Java]]` 자동 치환 + memory_links target_label 재인덱스 ✅
- 프로젝트 부트스트랩: PRD, ARCHITECTURE, ADR(001~019), ROADMAP, UI_GUIDE, HANDOFF, DEVELOPMENT, SECURITY, CONTRIBUTING 문서 작성
- 도메인 모델 명세 (Memory/Folder/Tag/MemoryLink/Memory ID)
- 환경 변수 전체 명세 (`deploy/.env.example`)
- 보안 정책 (격리, rate limit, 토큰 한도, 헤더, PII 마스킹)
- 데이터 포터빌리티 정책 (export + import)
- MIT 라이선스
- MVP 정체성 확장: 본문 `[[wiki-link]]` 파싱 + `/map` 그래프 + `/archive` + `/keys` MCP 명령 빌더 (ADR-018, ADR-019)
- phase 16 wiki-link-parser, phase 17 memory-map-ui
- Phase 01 project-skeleton:
  - Gradle 8.7 + Kotlin 1.9.25 + Spring Boot 3.3.4 백엔드 모듈
  - Vite 5 + React 18 + TypeScript 5 + Tailwind 3 프론트엔드 모듈
  - Docker Compose 스택: postgres(pgvector pg16) + backend + frontend-dev + caddy
  - GitHub Actions CI: ktlint, JUnit + Kotest, ESLint, Vitest, build 검증
  - 백엔드 스모크 테스트, 프론트엔드 App 렌더 테스트
- 디자인 정책: 중립 모노크롬 다크 테마 확정(`docs/design/` 참고 이미지 2장 추가)
- Phase 02 persistence-base:
  - JPA + Flyway + pgvector-java + HikariCP 의존성 도입
  - Flyway V1__init.sql: 13 테이블, vector/pg_trgm 확장, ivfflat 임베딩 인덱스, tsv 트리거
  - id 모듈: UUID v7 생성기 + Crockford Base32(26자) + PrefixedId(11 prefix) + IdFactory
  - 도메인 엔티티: User, ApiKey, Folder, Memory(@Version), MemoryLink, Tag, MemoryTag
  - JPA 리포지토리 7개: 모든 조회 메서드 첫 인자 userId 강제 (격리 시그니처 기반)
  - 단위 테스트: id 모듈 3개 클래스 PASSED, smoke 유지
- Phase 03 auth-google-oauth (코드 완료, 라이브 검증 사용자 액션 대기):
  - Spring Security 5 + OAuth2 client 의존성, SecurityConfig 기본 필터 체인
  - Conditional Google ClientRegistrationRepository (env 비어 있으면 빈 미생성)
  - OAuth2LoginSuccessHandler: Google sub → users 테이블 upsert
  - 세션 쿠키 MNEME_SESSION(HttpOnly + SameSite=Lax, prod Secure)
  - CSRF(CookieCsrfTokenRepository), CORS allowlist(MNEME_FRONTEND_ORIGIN)
  - 보안 헤더 CSP/X-Frame-Options/X-Content-Type-Options/Referrer-Policy/Permissions-Policy
  - ApiError enum + GlobalExceptionHandler(ProblemDetail/RFC 7807) + i18n/errors_ko.properties
- Phase 04 api-keys:
  - ApiKeyGenerator: mn_<base62 32B> 평문 + sha256 해시 + 8자 식별 prefix + constant-time verify
  - ApiKeyService: issue/listActive/revoke/rename/rotate
  - REST: POST/GET/PATCH/DELETE /api/keys, POST /api/keys/{extId}/rotate (평문 1회 IssueResponse)
  - ApiKeyAuthenticationFilter (Bearer mn_): prefix 후보 → keyHash verify → SecurityContext
  - AuditEvent 엔티티 + AuditPublisher(REQUIRES_NEW @Transactional), key.created/revoked 기록
- Phase 05 memory-domain:
  - FolderService + /api/folders REST (materialized path 자동, 이름 중복 409)
  - MemoryService + /api/memories REST: create/get/update(@Version 낙관적 락)/archive/restore/list, 본문 256KB 상한
  - TagService + /api/tags + /api/memories/{}/tags: 정규화 소문자, 32자, 메모리당 16개 상한
  - AuthenticatedUserResolver: ApiKeyAuthenticationToken 또는 OAuth2User → userId
  - IsolationRegressionTest 단위 베이스(folder/memory/tag): 다른 userId 접근 시 404 검증
- Phase 14 observability (code + live ✅):
  - `micrometer-registry-prometheus` 런타임 + `management.endpoints.web.exposure.include=health,prometheus` + `SecurityConfig` `/actuator/prometheus` 익명 허용
  - `AuditController` `GET /api/audit` — 본인 이벤트(user_id/ip/user_agent 응답 노출 금지) createdAt desc
  - `UsageDailyRepository` 네이티브 SELECT + `UsageController` `GET /api/usage?from=&to=`(기본 최근 30일)
  - 프론트 `/audit`(이벤트 리스트) + `/usage`(요약 카드 + 일별 표) + Shell 탭 추가
  - 라이브 2026-06-28: prometheus 200, audit 2건, usage 1일자 정상 ✅
- Phase 13 export-import (code + live ✅):
  - `ExportService` — 사용자 폴더/태그/메모리(active+archived)를 zip 스트림으로 직렬화. `manifest.json` + `memories/<extId>.md`(frontmatter)
  - `ExportController` `GET /api/export` application/zip + Content-Disposition
  - `ImportService` 2단계 — `preview`(.md 파싱 + 충돌 표시 + Caffeine 30분 TTL session) + `apply`(항목별 skip/replace/create-new). `ensureFolder` 자동 폴더 트리 생성
  - `ImportController` POST `/api/import/preview`(multipart) + `/api/import/apply`(JSON)
  - 프론트 `/data` 페이지 — Export 다운로드 + Import 업로드 + 항목별 충돌 결정 UI + Shell 헤더 '데이터' 탭
  - 라이브 2026-06-28: 5건 메모리 export zip + 5건 preview + skip 5건 apply 정상 ✅
- Phase 12 onboarding-guide (code ✅, **M6 달성**):
  - `OnboardingTour` 첫 로그인 4단계 모달 + localStorage `mneme.onboarded` 플래그
  - `/connect` 페이지 — Claude Desktop/Codex CLI/ChatGPT Developer mode 3종 스니펫 + 복사 + 스크린샷 자리
  - Shell 헤더 "연결" 탭
- Phase 11 dashboard-ui (code + live ✅):
  - step 2 editor: `react-markdown` + `remark-gfm` + `@tailwindcss/typography` + `MarkdownView`. `MemoryDetailPage` 뷰/편집 + PATCH 낙관적 락 + 409 ConflictPanel(서버/내 본문 좌우 + keep mine/take server/manual merge) + 보관 액션
  - step 3 search/archive/keys: `SearchBar`+`SearchPage`(/search?q=) + `ArchivePage` + `KeysPage` 발급/폐기/회전 + Claude Desktop/Codex MCP 연결 명령 빌더. `ApiKeyController` Bearer 통일. V3 마이그레이션으로 `audit_events.ip` INET→TEXT
  - step 4 shortcuts: `useShortcut` 훅(Mod+K 검색바 포커스)
  - 라이브: 편집/충돌/검색/발급/폐기/아카이브 정상 ✅
- Phase 11 step 1 dashboard-ui shell:
  - 의존성 `react-router-dom`@6 + `@tanstack/react-query`@5
  - Tailwind 모노크롬 토큰(`ink-50…ink-900`, 보라/푸른기 금지)
  - `lib/auth.ts` + `api/client.ts` + `api/{folders,memories}.ts`
  - `AuthGate`, `Shell`, `Sidebar`, `MemoryListPage`, `MemoryDetailPage`, `StubPage`
  - React Router 라우트 5종 + smoke 테스트 갱신 + jsdom localStorage stub
  - npm typecheck/lint/test/build + Vite dev 5173 정상. 백엔드 REST Bearer 통과 ✅
- Phase 10 mcp-oauth-dcr (code + live ✅, **M5 달성**):
  - Flyway V2: `oauth_clients.user_id` NULL 허용(DCR 익명 등록)
  - `OAuthClient`/`OAuthToken` 엔티티 + 리포지토리 + `OAuthProperties`(access 30분/refresh 30일/code 10분)
  - `OAuthService` — `registerClient`(DCR) / `issueCode`(Caffeine 10분 TTL) / `exchangeCode`(PKCE S256) / `refresh`(회전) / `authenticateAccessToken`. 시크릿·토큰 모두 sha256만 저장
  - `OAuthController` `/oauth/{register,authorize,token}` — RFC 7591/6749 호환
  - `OAuthAccessTokenAuthenticationFilter` — `Bearer` non-`mn_` Bearer를 oauth_tokens 조회 후 SecurityContext 주입
  - SecurityConfig: `/oauth/register`, `/oauth/token` CSRF 면제 + filter chain 갱신
  - `OAuthServiceTest` 6건(DCR/PKCE/refresh/access auth/잘못된 verifier/다른 client 거부)
  - 라이브 2026-06-28: DCR(`oac_pk...`) + 시드 토큰 + `refresh_token` grant → 새 access/refresh + 새 access로 `/sse` + `mn_whoami`(`sdsd1008@gmail.com`) 정상 ✅
- Phase 09 mcp-server (code + live ✅, M5 절반):
  - `spring-ai-bom:1.0.0` + `spring-ai-starter-mcp-server-webmvc` 의존성. application.yml `spring.ai.mcp.server.*`
  - `MnemeTools`: 11개 `mn_*` 도구(`mn_schema/mn_whoami/mn_list/mn_read/mn_search/mn_write/mn_update/mn_archive/mn_restore/mn_relations/mn_surface`)
  - `McpToolsConfig`: `MethodToolCallbackProvider` 빈 등록
  - `McpContextPropagationConfig`: Micrometer Context Propagation에 SecurityContext `ThreadLocalAccessor` 등록 + `Hooks.enableAutomaticContextPropagation()` — Reactor boundedElastic 워커에 SecurityContextHolder 전파
  - `McpToolsIsolationTest` 6건(read/archive/restore/relations 404 + whoami 본인만)
  - 라이브 2026-06-28: GET `/sse` endpoint event 수신 → POST `/mcp/message`로 initialize + tools/list(11개 inputSchema) + mn_whoami(`sdsd1008@gmail.com`) + mn_list(폴더/메모리) + mn_search(`코틀린` → 코틀린 메모 score 0.367 1위) 모두 정상 ✅
- Phase 08 security-controls (code + live ✅, M4 도달):
  - `RateLimitService` Caffeine 분/일/쓰기 분리 카운터 + `RateLimitFilter` 429 ProblemDetail
  - `mneme.rate-limit.*` 설정(per-min/per-day/write-per-min/anonymous-per-min)
  - `TokenQuotaGuard`: usage_daily 조회 후 임베딩/채팅 호출 직전 한도 검사 → 초과는 `OpenAiException.RateLimited`로 변환
  - `mneme.token-quota.*` 설정(embed-per-day=100k / llm-per-day=50k 기본)
  - `PiiMaskingConverter` (logback): mn_*, sk-*, 이메일 자동 마스킹. `%maskedMsg` 패턴
  - `IsolationRegressionTest` 확장(api key + memory archive 케이스)
  - 라이브 2026-06-28: 쓰기 21번째 429 차단 + embed 100k 초과 시 임베딩 skip(본문 저장 계속) 확인
- Phase 07 hybrid-search (code + live ✅):
  - `SearchService` 단일 네이티브 SQL — α·(1−cosine) + β·ts_rank + γ·similarity
  - `SearchProperties` (`mneme.search.*` 가중치 α=0.6 / β=0.3 / γ=0.1 + limit 1..100)
  - `GET /api/search?q=&folderExtId=&tags=&from=&to=&limit=` (응답은 본문 제외)
  - JPA 네이티브 timestamp → OffsetDateTime 변환 헬퍼
  - 라이브 검증 2026-06-28: 의미 기반 정렬 정상(벡터 검색 질의 → pgvector 메모리 1위)
- Phase 06 llm-adapter (code + live ✅):
  - OpenAI REST 어댑터(`OpenAiClient` + `LlmProperties` + `OpenAiException` 계층). ADR-020.
  - `EmbeddingService` text-embedding-3-small 1536d + SHA-256 키 Caffeine 캐시(5000/30min)
  - `PromptGuard.fence` 8KB 절단 + 마커 이스케이프 펜스(프롬프트 인젝션 1차 방어)
  - `ChatService` gpt-4o-mini: `summarize`/`classifyFolder`/`suggestTags` + 시스템 프롬프트 리소스 분리
  - `MemoryWriteFacade`: 외부 호출은 트랜잭션 밖 → MemoryService(tx1) → `MemoryEmbeddingDao` 네이티브 UPDATE(tx2)
  - `PgUsageRecorder` REQUIRES_NEW: `usage_daily` ON CONFLICT 일일 토큰 집계
  - SecurityConfig: Authorization Bearer 헤더 보유 요청은 CSRF 면제(phase 04 보안 보정)
  - 라이브 검증 2026-06-28: embedding 1536d 정상 저장 + 요약 자동 생성 + usage_daily 집계 정상

### Changed
- 프로젝트명을 `unified-memory` → `Mneme`로 변경
- Kotlin 패키지 루트 `com.mneme`
- ADR-003 개정: Heirmos 골격 + LLM Wiki 본문 [[link]]를 MVP 핵심 정체성으로 채택
- phase 20 wiki-links를 deferred → MVP phase 16으로 승격

### 향후 마일스톤 (미릴리즈)

#### M1 — 로컬 부팅
- phase 01 ✅ / phase 02 ✅ → 0.1.0 후보 도달

#### M4 — 보안 완전
- phase 08 ✅ → 0.4.0 후보 도달

#### M5 — MCP 라이브
- phase 09 ✅ + phase 10 ✅ → 0.5.0 후보 도달

#### M7 — 포터빌리티·운영
- phase 13-15 완료 시 1.0.0-rc 후보
