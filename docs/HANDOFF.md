# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-27 (phase 01 project-skeleton 완료)
- 직전 작업:
  - Gradle 8.7 + Kotlin 1.9.25 + Spring Boot 3.3.4 백엔드 골격 (com.mneme 13개 패키지 스캐폴딩)
  - Vite 5 + React 18 + TS 5 + Tailwind 3 프론트엔드 골격 (placeholder App)
  - Docker Compose 스택: postgres(pgvector pg16) + backend + frontend-dev + caddy, 5종 헬스 검증 통과(actuator 직접/caddy 경유, frontend 직접/caddy 경유, pg_isready)
  - GitHub Actions CI: backend(ktlint + test + build) + frontend(lint + typecheck + test + build) 2개 job, Dependabot 주 1회
  - 스모크 테스트: backend MnemeApplicationSmokeTest 2케이스, frontend App 렌더 1케이스
  - 디자인 정책 픽스: 모노크롬 그레이 스케일(보라/푸른기 사용 안 함)
  - LLM Wiki 본문 [[link]] MVP 핵심 정체성 채택, phase 16/17 신규
- 작성자 노트: phase 02 persistence-base 진입 — Flyway V1__init.sql + JPA 엔티티 + pgvector 확장 활성화 + HikariCP + Testcontainers

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
| 02 persistence-base | pending | Flyway V1 + 엔티티 + pgvector + HikariCP + Testcontainers |
| 03~15 | pending | ROADMAP 참조 (MVP 골격) |
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

### 우선순위 1: phase 02 persistence-base 설계 및 실행

1. `phases/02-persistence-base/index.json`과 stepN.md 작성
2. backend `build.gradle.kts`에 추가: spring-boot-starter-data-jpa, postgresql JDBC, flyway-core, com.pgvector(pgvector-java)
3. `application.yml`에서 DataSource autoconfigure exclude 두 줄 제거
4. `db/migration/V1__init.sql` 작성: users / api_keys / oauth_clients / oauth_tokens / sessions / folders / memories / tags / memory_tags / memory_links / memory_versions / audit_events / usage_daily + 인덱스 + pgvector 확장
5. 도메인 엔티티 + 리포지토리 (user_id 강제 시그니처)
6. Testcontainers + Postgres+pgvector 이미지로 통합 테스트 인프라
7. HikariCP 설정 적용
