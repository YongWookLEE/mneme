# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-27 (브레인스토밍 + 심층 보강 + 디렉토리 rename 종료)
- 직전 작업:
  - 프로젝트 디렉토리 `unified-memory` → `mneme` 변경
  - 프로젝트명 Mneme 확정, Kotlin 패키지 `com.mneme`
  - PRD/ARCHITECTURE/ADR/ROADMAP/UI_GUIDE/CLAUDE/AGENTS 전면 보강
  - 신규 문서: `docs/DEVELOPMENT.md`, `docs/SECURITY.md`, `docs/CONTRIBUTING.md`, `CHANGELOG.md`, `LICENSE`, `deploy/.env.example`
  - 신규 ADR: 012(ID 형식) / 013(폴더 vs 태그) / 014(Flyway forward-only) / 015(CI) / 016(낙관적 락) / 017(i18n)
  - 도메인 모델 명세 추가(Memory/Folder/Tag/Memory ID), 환경 변수 전체 목록, 트랜잭션 경계, 동시성 정책, HikariCP 설정, HTTP 상태 코드 매핑, 키보드 단축키 표
- 작성자 노트: 사용자 docs 리뷰 → `phases/01-project-skeleton/` 실행 계획 → 코드 골격. Kotlin 패키지 `com.mneme`로 통일

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

자세한 결정 근거는 `docs/ADR.md` (ADR-001 ~ ADR-017).

## Phase 진행 상태

| Phase | 상태 | 메모 |
|-------|------|------|
| 00 docs-bootstrap | completed | 전체 문서 본문 + 보안·사용성·도메인 모델 + 추가 문서(DEVELOPMENT/SECURITY/CONTRIBUTING/LICENSE/CHANGELOG/.env.example). 사용자 리뷰 대기 |
| 01 project-skeleton | pending | Spring Boot + Vite 골격, docker-compose, CI 초기 |
| 02 persistence-base | pending | Flyway V1 + 엔티티 + pgvector + HikariCP + Testcontainers |
| 03~15 | pending | ROADMAP 참조 (MVP 16개 phase) |
| 20~23 | deferred | Wiki 확장 |
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

### 우선순위 1: 사용자 docs 리뷰 반영

1. PRD/ARCHITECTURE/ADR/ROADMAP/UI_GUIDE/DEVELOPMENT/SECURITY 검토
2. 수정 요청 반영
3. 커밋: `docs: bootstrap project docs with deep security and ux coverage`

### 우선순위 2: phase 01 설계 및 실행

1. `phases/01-project-skeleton/index.json`과 `step1.md`~`step5.md` 작성
2. `backend/` Gradle Kotlin 부트스트랩 (Spring Boot 3.x, JDK 21, Spring Web, Spring AI MCP Server, Spring Security, Spring Data JPA, Flyway, pgvector, Micrometer, Caffeine, Testcontainers, Kotest)
3. `frontend/` Vite + React + TypeScript + shadcn/ui + TanStack Query + react-i18next + Playwright 부트스트랩
4. `deploy/docker-compose.yml` + `Caddyfile` + Postgres+pgvector 이미지
5. `.github/workflows/ci.yml` 초기 (lint + test + build)
6. `docker compose up`으로 health 체크 (백엔드 `/actuator/health` 200, 프론트 5173 접근, Postgres 5432 연결)
7. CHANGELOG.md에 M1 항목 추가
