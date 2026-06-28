# Changelog

[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 형식, [Semantic Versioning](https://semver.org/spec/v2.0.0.html) 지향.

## [Unreleased]

### Added
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
