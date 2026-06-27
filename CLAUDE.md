# 프로젝트: Mneme

> Mneme — 그리스 기억의 뮤즈. 여러 AI 클라이언트가 공유하는 단일 영구 기억층.

이 저장소는 Basic Harness 템플릿을 사용한다. Claude는 이 파일을 프로젝트 헌법으로 우선 읽고, `docs/`와 `phases/`의 지시를 함께 따라야 한다.

## 기술 스택

- Spring Boot 3.x + Spring AI MCP Server + Spring Security OAuth2 + Spring Data JPA + Micrometer + Caffeine
- Kotlin (JDK 21, strict null safety), Kotlin 패키지 루트 `com.mneme`
- PostgreSQL 16 + pgvector + pg_trgm 확장 (메타데이터·전문검색·벡터 단일 DB)
- 백엔드 테스트: JUnit5 + Kotest assertions + Testcontainers
- 프론트엔드: React 18 + Vite + TypeScript + shadcn/ui + TanStack Query + react-i18next + Vitest + Playwright(E2E)
- 빌드: Gradle (Kotlin DSL) + npm
- 개발/배포: Docker Compose (Spring Boot + Postgres+pgvector + Caddy + Vite dev)
- CI: GitHub Actions (lint + test + build, 배포는 manual approve)

## 아키텍처 규칙

- CRITICAL: **사용자 데이터 격리**. 모든 리포지토리 메서드는 user_id를 첫 인자로 명시. 통합 테스트 `IsolationRegressionTest`가 모든 11개 도구·REST 엔드포인트에서 교차 접근 → 404 자동 검증. 신규 엔드포인트 추가 시 이 테스트에 케이스 추가 의무. 권한 위반은 항상 404.
- CRITICAL: **메모리는 soft delete만**(`archived_at`). 영구 DELETE 금지. 사용자 계정 삭제 시에만 30일 유예 후 완전 제거.
- CRITICAL: **API 키 평문 저장 금지**. sha256 해시 + prefix(앞 8자) 식별만. 발급 응답에서만 1회 노출.
- CRITICAL: **SQL/프롬프트에 사용자 입력 직접 보간 금지**. prepared statement / Spring AI 메서드 인자 / 인용 블록.
- CRITICAL: **로그·에러 응답에 평문 키·토큰·메모리 본문·임베딩 노출 금지**. 자동 마스킹 미들웨어 적용.
- CRITICAL: **MCP 도구 이름은 `mn_` prefix 유지**.
- CRITICAL: **Rate limit·일일 LLM 토큰 한도 우회 금지**. 임계치 초과는 호출 차단 + 알림.
- CRITICAL: **컨트롤러에서 `@Transactional` 사용 금지**. 트랜잭션 경계는 서비스 레이어. 외부 호출(OpenAI/이메일)은 트랜잭션 밖에서.
- CRITICAL: **Flyway 마이그레이션은 forward-only**. rollback 스크립트 작성 금지. 잘못된 마이그레이션은 새 버전으로 보정.
- CRITICAL: **외부 노출 ID는 base32 prefix 형식**(`mem_`/`fld_`/`tag_`/`key_`). 내부 UUID 그대로 노출 금지.
- 컴포넌트, 타입, 유틸리티, 테스트는 `docs/ARCHITECTURE.md` 디렉터리 구조 준수.
- ADR에 기록된 기술 선택을 임의로 바꾸지 않는다. 변경 시 이유·트레이드오프 먼저 문서화.

## 개발 프로세스

- CRITICAL: 새 기능 구현 시 반드시 테스트를 먼저 작성하고, 테스트가 통과하는 구현을 작성한다.
- 새 세션 시작 시 `docs/HANDOFF.md`를 먼저 읽어 현재 phase/step과 다음 작업을 파악한다.
- 각 step은 `phases/{task}/stepN.md`에 명시된 범위만 수행한다. 추가 기능이나 파일을 만들지 않는다.
- Acceptance Criteria의 검증 명령을 직접 실행하고 결과를 확인한다.
- 작업 종료 직전 `docs/HANDOFF.md`를 갱신한다.
- 마일스톤 도달 시 `CHANGELOG.md`에 항목을 추가한다.
- 커밋 메시지는 conventional commits 형식: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `security:`, `perf:`.

## Phase 상태 관리

- `phases/{task}/index.json`의 현재 step 상태를 작업 결과에 맞게 갱신한다.
- 성공: `"status": "completed"` + `"summary"` 한 줄.
- 3회 시도 후에도 실패: `"status": "error"` + 구체적 `"error_message"`.
- 사용자 개입 필요(API 키, OAuth, 수동 설정): `"status": "blocked"` + `"blocked_reason"` 후 중단.
- `created_at`, `started_at`, `completed_at`, `failed_at`, `blocked_at`은 실행기가 관리.

## 문서 게이트

- 다음 파일은 항상 완성 상태를 유지한다: `docs/PRD.md`, `docs/ARCHITECTURE.md`, `docs/ADR.md`, `docs/UI_GUIDE.md`, `docs/ROADMAP.md`, `docs/HANDOFF.md`, `docs/DEVELOPMENT.md`, `docs/SECURITY.md`, `docs/CONTRIBUTING.md`, `CLAUDE.md`, `AGENTS.md`, `README.md`, `CHANGELOG.md`.
- 문서 안에 미완성 토큰을 남기지 않는다.

## 금지 명령

- `rm -rf`
- `git push --force`
- `git reset --hard`
- `DROP TABLE`
- Flyway baseline 재설정
- 운영 DB에 직접 DDL

위 명령 또는 같은 효과를 내는 파괴적 작업은 사용자 명시 승인 없이 실행하지 않는다.

## 명령어

- `docker compose up -d` - 로컬 전체 스택 (백엔드 8080, 프론트 5173, Postgres 5432, Caddy 443/80)
- `docker compose logs -f backend` - 백엔드 로그 추적
- `./gradlew :backend:bootRun` - 백엔드 단독 개발 실행
- `npm --prefix frontend run dev` - 프론트엔드 dev 서버 단독
- `./gradlew :backend:build` - 백엔드 빌드 (jar)
- `npm --prefix frontend run build` - 프론트엔드 정적 빌드
- `./gradlew :backend:ktlintCheck` / `:backend:ktlintFormat` - 백엔드 린트
- `npm --prefix frontend run lint` / `lint:fix` - 프론트엔드 린트
- `./gradlew :backend:test` - 백엔드 단위 + 통합 (Testcontainers)
- `./gradlew :backend:test --tests "*IsolationRegressionTest"` - 격리 회귀 테스트만
- `npm --prefix frontend run test` - 프론트엔드 단위 테스트
- `npm --prefix frontend run e2e` - Playwright E2E
- `./gradlew :backend:flywayMigrate` - 마이그레이션 수동 실행 (로컬 디버깅)
- `docker compose exec postgres psql -U mneme -d mneme` - DB 접속
