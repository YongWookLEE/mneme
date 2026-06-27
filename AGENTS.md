# 프로젝트: Mneme

> Mneme — 그리스 기억의 뮤즈. 여러 AI 클라이언트가 공유하는 단일 영구 기억층.

이 저장소는 Basic Harness 템플릿을 사용한다. Codex는 이 파일을 프로젝트 헌법으로 우선 읽고, `docs/`와 `phases/`의 지시를 함께 따라야 한다.

## 기술 스택

- Spring Boot 3.x + Spring AI MCP Server + Spring Security OAuth2 + Spring Data JPA + Micrometer + Caffeine
- Kotlin (JDK 21, strict null safety), Kotlin 패키지 루트 `com.mneme`
- PostgreSQL 16 + pgvector + pg_trgm
- 백엔드 테스트: JUnit5 + Kotest assertions + Testcontainers
- 프론트엔드: React 18 + Vite + TypeScript + shadcn/ui + TanStack Query + react-i18next + Vitest + Playwright
- 빌드: Gradle (Kotlin DSL) + npm
- 개발/배포: Docker Compose
- CI: GitHub Actions

## 아키텍처 규칙 (CRITICAL)

- 사용자 데이터 격리: 모든 리포지토리 메서드 user_id 첫 인자. `IsolationRegressionTest`로 자동 검증. 권한 위반은 404.
- 메모리는 soft delete만(`archived_at`). 영구 DELETE 금지.
- API 키 평문 저장 금지. sha256 + prefix 8자.
- SQL/프롬프트에 사용자 입력 직접 보간 금지. prepared statement / 인용 블록.
- 로그·에러에 평문 키·토큰·메모리 본문·임베딩 노출 금지.
- MCP 도구 이름 `mn_` prefix 유지.
- Rate limit·일일 토큰 한도 우회 금지.
- 컨트롤러 `@Transactional` 사용 금지. 외부 호출은 트랜잭션 밖에서.
- Flyway forward-only. rollback 스크립트 금지.
- 외부 노출 ID는 base32 prefix(`mem_`/`fld_`/...). 내부 UUID 노출 금지.
- 디렉터리 구조는 `docs/ARCHITECTURE.md` 준수.
- ADR 결정 임의 변경 금지.

## 개발 프로세스

- 새 기능은 테스트 우선.
- 세션 시작 시 `docs/HANDOFF.md` 먼저 읽기.
- 각 step은 `phases/{task}/stepN.md` 범위만.
- Acceptance Criteria 명령을 직접 실행해 검증.
- 작업 종료 직전 `docs/HANDOFF.md` 갱신.
- 마일스톤 도달 시 `CHANGELOG.md` 갱신.
- 커밋: conventional commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `security:`, `perf:`).

## Phase 상태 관리

- 성공: `"status": "completed"` + `"summary"`.
- 실패(3회 후): `"status": "error"` + `"error_message"`.
- 사용자 개입 필요: `"status": "blocked"` + `"blocked_reason"` 후 중단.

## 문서 게이트

- 항상 완성 유지: `docs/PRD.md`, `docs/ARCHITECTURE.md`, `docs/ADR.md`, `docs/UI_GUIDE.md`, `docs/ROADMAP.md`, `docs/HANDOFF.md`, `docs/DEVELOPMENT.md`, `docs/SECURITY.md`, `docs/CONTRIBUTING.md`, `CLAUDE.md`, `AGENTS.md`, `README.md`, `CHANGELOG.md`.
- 미완성 토큰 금지.

## 금지 명령

- `rm -rf`, `git push --force`, `git reset --hard`, `DROP TABLE`
- Flyway baseline 재설정, 운영 DB 직접 DDL

## 명령어

- `docker compose up -d` - 로컬 전체 스택
- `./gradlew :backend:bootRun` - 백엔드 단독
- `npm --prefix frontend run dev` - 프론트엔드 dev
- `./gradlew :backend:build` / `npm --prefix frontend run build` - 빌드
- `./gradlew :backend:ktlintCheck` / `npm --prefix frontend run lint` - 린트
- `./gradlew :backend:test` - 백엔드 테스트 전체
- `./gradlew :backend:test --tests "*IsolationRegressionTest"` - 격리 회귀
- `npm --prefix frontend run test` / `e2e` - 프론트엔드 테스트
- `docker compose exec postgres psql -U mneme -d mneme` - DB 접속
