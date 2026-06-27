# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-27 (Heirmos UI 검토 → LLM Wiki 패턴 채택 → MVP 범위 확장 문서 갱신)
- 직전 작업:
  - 사용자가 Heirmos 화면 5종 공유 + LLM Wiki 패턴 공유 → MVP 범위 재논의
  - 결정: 본문 `[[wiki-link]]` 파싱을 MVP 핵심으로 승격(옵시디언 경험 차용). 별도 관계 테이블 CUD 도구 안 만듦 — 본문이 진실, `memory_links`는 파생 인덱스
  - 결정: `/map`(관계 그래프), `/archive`(보관함), `/keys`에 MCP 명령 빌더 → MVP UI 추가
  - 결정: Docker-first 정책 확정(로컬·배포 모두 컨테이너)
  - 신규 ADR: 018(본문 [[link]]가 관계의 진실) / 019(/map 명명·도구적 톤 유지)
  - ADR-003 개정(Heirmos 골격 + LLM Wiki 본문 링크 MVP 핵심)
  - PRD 핵심 기능 #5 재정의, MVP 범위에 `/map`·`/archive`·`/keys` 빌더 추가, MVP 제외에서 wiki-link 파싱 제거
  - UI_GUIDE 페이지 추가(`/map`, `/archive`), 본문 [[link]] UX 절 추가, 단축키 `Cmd/Ctrl+G` 추가
  - ARCHITECTURE `memory_links` 스키마 보강(user_id, target_label, broken 인덱스), 메모리 저장 흐름에 파싱 단계 + 제목 변경 시 backlink 일괄 치환 절차 추가
  - ROADMAP에 phase 16 wiki-link-parser, phase 17 memory-map-ui 추가. phase 20 wiki-links는 16으로 승격(제거)
- 작성자 노트: 다음 작업 — phase 01 step 1 (gradle/spring boot 골격) 시작. 위 변경은 phase 01 범위(인프라 골격)에 영향 없음

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
| 01 project-skeleton | pending | Spring Boot + Vite 골격, docker-compose, CI 초기 |
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

### 우선순위 1: phase 01 step 1 실행 (인프라 골격, Docker-first)

1. `docker run --rm -v $PWD:/app -w /app gradle:8.7-jdk21 gradle wrapper --gradle-version 8.7 --distribution-type bin`로 wrapper 생성
2. 루트 `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `backend/build.gradle.kts` 작성
3. `backend/src/main/kotlin/com/mneme/MnemeApplication.kt` + 패키지 `.gitkeep` 13개
4. `application.yml` + `application-local.yml` + `application-prod.yml` + `logback-spring.xml`
5. `.gitignore` 보강
6. 컨테이너 안에서 `./gradlew :backend:build -x test` + `:backend:ktlintCheck` → BUILD SUCCESSFUL
7. `:backend:bootRun` + `curl /actuator/health` → `{"status":"UP"}`
8. `phases/01-project-skeleton/index.json` step 1 → completed, HANDOFF.md 갱신, 단일 커밋

### 우선순위 2: phase 01 step 2~6 진행

step1.md ~ step6.md 순서대로 진행. 각 step 완료 시 index.json 갱신.
