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

### Changed
- 프로젝트명을 `unified-memory` → `Mneme`로 변경
- Kotlin 패키지 루트 `com.mneme`
- ADR-003 개정: Heirmos 골격 + LLM Wiki 본문 [[link]]를 MVP 핵심 정체성으로 채택
- phase 20 wiki-links를 deferred → MVP phase 16으로 승격

### 향후 마일스톤 (미릴리즈)

#### M1 — 로컬 부팅
- phase 01 ✅ / phase 02 ✅ → 0.1.0 후보 도달

#### M5 — MCP 라이브
- phase 09-10 완료 시 0.5.0 후보

#### M7 — 포터빌리티·운영
- phase 13-15 완료 시 1.0.0-rc 후보
