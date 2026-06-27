# Phase 01: project-skeleton — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 이 파일은 phase 01의 전체 plan이며, 각 step의 세부는 `phases/01-project-skeleton/stepN.md`와 짝을 이룬다.

**Goal:** Mneme MVP의 코드 골격(Kotlin Spring Boot 백엔드 + React/Vite 프론트엔드 + Postgres+pgvector + Caddy)을 만들고, `docker compose up`만으로 모든 컴포넌트가 헬스 통과하며, CI가 lint/test/build를 돌리는 상태까지 가져간다.

**Architecture:** 단일 저장소에 `backend/`, `frontend/`, `deploy/` 세 모듈을 둔다. 백엔드는 Gradle Kotlin DSL 멀티프로젝트의 단일 모듈로 시작(차후 확장 여지). 프론트는 npm 워크스페이스 없이 `frontend/` 단독. 배포는 Compose 1개 파일 + 로컬 override 1개로 운영·로컬 동일 부팅. 첫 phase에서는 도메인 로직 없이 "골격 + 헬스 + 부팅"만 다루며 도메인은 phase 02부터.

**Tech Stack:** Kotlin 1.9.x + JDK 21 + Spring Boot 3.x + Spring Web + Spring Actuator / React 18 + Vite 5 + TypeScript 5 + Tailwind 3 / postgres:16 + pgvector / Caddy 2 / GitHub Actions / Docker Compose v2.

## Global Constraints

- 모든 코드는 `docs/ARCHITECTURE.md`의 디렉터리 구조를 따른다. 신규 파일을 다른 위치에 두지 않는다.
- 백엔드 Kotlin 패키지 루트는 `com.mneme`. JDK 21. Spring Boot 3.x. (ADR-001)
- 환경 변수 prefix는 `MNEME_*`. 노출 변수 목록은 `deploy/.env.example`과 일치해야 한다.
- 컨트롤러에 `@Transactional` 금지. (ADR-009, CLAUDE.md CRITICAL)
- 새로 작성하는 Kotlin 함수/클래스에는 한국어 KDoc 주석을 단다(`/** ... */`). (전역 코딩 스타일)
- 새로 작성하는 TypeScript 함수/컴포넌트에는 JSDoc 한국어 주석을 단다.
- Flyway 마이그레이션은 이 phase에서 작성하지 않는다(phase 02 범위).
- 비밀 파일(`.env`, `secrets/`) git 커밋 금지. `.gitignore`에 등록.
- 이 phase의 작업은 phase 02 이후 phase 흐름을 막지 않도록 stub/placeholder가 아닌 "동작하는 최소"여야 한다.
- 각 step 종료 시 `phases/01-project-skeleton/index.json`의 해당 step `status`를 `completed`로 갱신하고 `summary` 한 줄을 남긴다.

---

## File Structure

### Task 1 (Gradle 루트 + backend 모듈)
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` (wrapper 8.x)
- Create: `backend/build.gradle.kts`
- Create: `backend/src/main/kotlin/com/mneme/MnemeApplication.kt`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-local.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `backend/src/main/resources/logback-spring.xml`

### Task 2 (백엔드 스모크 테스트)
- Create: `backend/src/test/kotlin/com/mneme/MnemeApplicationSmokeTest.kt`
- Create: `backend/src/test/resources/application-test.yml`

### Task 3 (프론트엔드 부트스트랩)
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/postcss.config.js`
- Create: `frontend/tailwind.config.ts`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/styles/globals.css`
- Create: `frontend/.eslintrc.cjs`
- Create: `frontend/src/__tests__/smoke.test.tsx`

### Task 4 (Docker Compose 스택)
- Create: `deploy/docker-compose.yml`
- Create: `deploy/docker-compose.override.yml`
- Create: `deploy/Caddyfile`
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile.dev`
- Modify: `deploy/.env.example` (이미 존재, 누락 변수 없는지만 확인 — 변경 없을 가능성 큼)

### Task 5 (컴포즈 헬스 검증)
- 신규 파일 없음. 모든 검증은 셸 명령으로 수행.

### Task 6 (CI + HANDOFF)
- Create: `.github/workflows/ci.yml`
- Modify: `docs/HANDOFF.md` (phase 01 → completed)
- Modify: `CHANGELOG.md` (M1 도달 항목 추가)

---

## Task 1: Gradle 루트 + backend Kotlin 모듈 부트스트랩

세부 단계는 [`step1.md`](step1.md) 참고. 요약:

- [ ] **1.1** Gradle wrapper 8.7 생성 (`gradle wrapper --gradle-version 8.7` 또는 수동 배치)
- [ ] **1.2** `settings.gradle.kts`, 루트 `build.gradle.kts`, `gradle.properties` 작성
- [ ] **1.3** `backend/build.gradle.kts` 작성 (Spring Boot 3.3.x, Kotlin 1.9.25, JDK 21, ktlint 12.x 플러그인, jvm 의존성)
- [ ] **1.4** `backend/src/main/kotlin/com/mneme/MnemeApplication.kt` + 패키지 디렉터리 (api/auth/memory/...) `.gitkeep`
- [ ] **1.5** `application.yml` / `application-local.yml` / `application-prod.yml` / `logback-spring.xml` 작성
- [ ] **1.6** `./gradlew :backend:build -x test` 통과 확인
- [ ] **1.7** `./gradlew :backend:ktlintCheck` 통과 확인
- [ ] **1.8** 커밋 `chore(backend): bootstrap kotlin spring boot skeleton`

**Interfaces produced (Task 1 → 다음 태스크):**
- `com.mneme.MnemeApplication`: `@SpringBootApplication` 메인 클래스. 빈 컨텍스트로 부팅 + Actuator `/actuator/health` 200 반환.
- `application.yml`: 공통 설정. `spring.profiles.active` 환경변수 `SPRING_PROFILES_ACTIVE`로 주입. management.endpoints.web.exposure.include=health.
- `application-local.yml`: 로컬 DB url 기본값 `jdbc:postgresql://localhost:5432/mneme` (compose 내부에서는 override).
- 루트 task `:backend:build`, `:backend:test`, `:backend:ktlintCheck`, `:backend:bootRun` 사용 가능.

---

## Task 2: 백엔드 스모크 테스트

세부 단계는 [`step2.md`](step2.md). 요약:

- [ ] **2.1** `MnemeApplicationSmokeTest.kt` 작성 — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`로 `/actuator/health` 200 확인
- [ ] **2.2** `application-test.yml` 작성 — 외부 의존성(OpenAI/Google) 환경변수 더미값, DataSource autoconfiguration 제외(이 phase에서는 DB 미사용)
- [ ] **2.3** `./gradlew :backend:test` 통과 확인 (1개 테스트)
- [ ] **2.4** 커밋 `test(backend): add application smoke test`

**Interfaces produced:** `MnemeApplicationSmokeTest` 패턴 — 이후 phase에서 통합 테스트를 추가할 때 동일한 import/구조를 재사용.

---

## Task 3: 프론트엔드 Vite + React + TypeScript 부트스트랩

세부 단계는 [`step3.md`](step3.md). 요약:

- [ ] **3.1** `frontend/package.json` 작성 (React 18, Vite 5, TypeScript 5, Tailwind 3, ESLint 8, Vitest 1, @testing-library/react)
- [ ] **3.2** `tsconfig.json` / `tsconfig.node.json` 작성 (strict, JSX react-jsx, target ES2022)
- [ ] **3.3** `vite.config.ts` 작성 (server.port 5173, proxy `/api` → `http://localhost:8080`)
- [ ] **3.4** `index.html` + `src/main.tsx` + `src/App.tsx` + `src/styles/globals.css` 작성 (Tailwind base, "Mneme — coming soon" 단순 페이지)
- [ ] **3.5** `tailwind.config.ts` + `postcss.config.js` 작성
- [ ] **3.6** `.eslintrc.cjs` 작성 (eslint-plugin-react-hooks + @typescript-eslint)
- [ ] **3.7** `src/__tests__/smoke.test.tsx` — App 컴포넌트 렌더 + "Mneme" 텍스트 존재
- [ ] **3.8** `npm --prefix frontend install`
- [ ] **3.9** `npm --prefix frontend run lint` 통과 확인
- [ ] **3.10** `npm --prefix frontend run test -- --run` 통과 확인
- [ ] **3.11** `npm --prefix frontend run build` 산출물 생성 확인
- [ ] **3.12** 커밋 `chore(frontend): bootstrap vite react typescript tailwind skeleton`

**Interfaces produced:**
- `npm --prefix frontend run dev` → 5173 포트 dev 서버.
- `npm --prefix frontend run build` → `frontend/dist/` 정적 산출물.
- `npm --prefix frontend run lint` / `test` / `e2e`(추후) 스크립트.

---

## Task 4: Docker Compose 스택 (postgres+pgvector, backend, frontend-dev, caddy)

세부 단계는 [`step4.md`](step4.md). 요약:

- [ ] **4.1** `backend/Dockerfile` 작성 (multi-stage: gradle build → eclipse-temurin:21-jre, 비루트 사용자)
- [ ] **4.2** `frontend/Dockerfile.dev` 작성 (node:20-alpine, `npm ci && npm run dev -- --host`)
- [ ] **4.3** `deploy/docker-compose.yml` 작성 — postgres(pgvector/pgvector:pg16), backend, caddy. healthcheck + depends_on 조건부
- [ ] **4.4** `deploy/docker-compose.override.yml` 작성 — frontend-dev 추가, backend 환경변수 override(`SPRING_PROFILES_ACTIVE=local`, `MNEME_DB_URL=jdbc:postgresql://postgres:5432/mneme`)
- [ ] **4.5** `deploy/Caddyfile` 작성 — `:80`에서 `/api/*`·`/actuator/*`·`/mcp/*`는 backend로, 나머지는 frontend-dev로 리버스 프록시. 로컬은 plain http.
- [ ] **4.6** `deploy/.env.example` 누락 변수 점검(현 파일과 ARCHITECTURE 매트릭스 대조 — 누락 없으면 변경 없음)
- [ ] **4.7** 커밋 `chore(deploy): add docker compose stack with postgres pgvector and caddy`

**Interfaces produced:**
- Compose 서비스 이름: `postgres`, `backend`, `frontend-dev`, `caddy`.
- 내부 네트워크: 기본 default(브리지). Postgres는 외부 포트 미노출(로컬 디버깅 시 override로만).
- Volume: `postgres_data` (named).

---

## Task 5: docker compose up 헬스 검증

세부 단계는 [`step5.md`](step5.md). 요약:

- [ ] **5.1** `cp deploy/.env.example deploy/.env`, 비밀 값을 더미로 채움(`MNEME_DB_PASSWORD=devpass`, OAuth/OpenAI는 빈 문자열 유지)
- [ ] **5.2** `docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml --env-file deploy/.env up -d --build`
- [ ] **5.3** `docker compose ps` — postgres `healthy`, backend `running`, caddy `running`, frontend-dev `running` 확인
- [ ] **5.4** `curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'` 통과
- [ ] **5.5** `curl -sf http://localhost:5173/` HTTP 200 확인
- [ ] **5.6** `curl -sf http://localhost/actuator/health` (Caddy 통과) 200 확인
- [ ] **5.7** `docker compose exec postgres pg_isready -U mneme` 통과
- [ ] **5.8** `docker compose down` 정리
- [ ] **5.9** 커밋 `chore(deploy): verify compose health smoke` (스택 변경 없으면 검증 노트만 step 메모에 기록)

**Interfaces produced:** 없음. 검증만.

---

## Task 6: GitHub Actions CI + 문서 갱신

세부 단계는 [`step6.md`](step6.md). 요약:

- [ ] **6.1** `.github/workflows/ci.yml` 작성 — PR/push 트리거. job 2개: `backend`(setup-java 21 + Gradle 캐시 + `./gradlew :backend:ktlintCheck :backend:test :backend:build`), `frontend`(setup-node 20 + npm ci + `lint` + `test -- --run` + `build`)
- [ ] **6.2** `docs/HANDOFF.md` 갱신 — "마지막 업데이트" 시각/요약 교체, Phase 진행 상태에서 `01 project-skeleton` → completed
- [ ] **6.3** `CHANGELOG.md` — `## [Unreleased]` 아래 `### Added`에 "Phase 01: 프로젝트 골격(Gradle/Kotlin 백엔드, Vite/React 프론트, Docker Compose, GitHub Actions CI)" 항목 추가. M1 도달 줄에 phase 01 체크.
- [ ] **6.4** `phases/01-project-skeleton/index.json`의 모든 step `status: completed` + `summary` 채우기
- [ ] **6.5** 커밋 `chore(ci): add github actions backend/frontend pipeline + docs: handoff phase 01 done`

---

## Self-Review (작성 직후 점검)

1. **스펙 커버리지**: HANDOFF "다음 세션 우선순위 2" 6항목과 ROADMAP phase 01 정의를 Task 1~6이 모두 다룬다 — backend(T1), frontend(T3), compose(T4), CI(T6), 헬스 검증(T5), CHANGELOG/HANDOFF(T6). ✓
2. **플레이스홀더**: 없음. 각 step.md에 실제 파일 내용·명령·기대 출력을 모두 적었다. ✓
3. **타입/이름 일관성**: Compose 서비스 이름(`postgres`/`backend`/`frontend-dev`/`caddy`), 포트(8080/5173/5432/80), 환경변수(`MNEME_*` + `SPRING_PROFILES_ACTIVE`)가 Task 1~6 모두에서 동일. ✓
4. **CRITICAL 위반**: Flyway 미작성(phase 02), DB 접속도 phase 01에서는 안 함(컨트롤러 없음, 엔티티 없음). 컨트롤러 `@Transactional` 없음. API 키 처리 없음. 모든 CRITICAL 회피. ✓

---

## Execution Handoff

이 plan을 어떻게 실행할지는 다음 메시지에서 선택:

1. **Subagent-Driven** — 매 task마다 fresh subagent 디스패치 + 두 단계 리뷰
2. **Inline Execution** — 이 세션에서 task 단위로 체크포인트 두며 진행
