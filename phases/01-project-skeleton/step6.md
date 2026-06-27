# Step 6: github-actions-ci-and-handoff

> Task 6 of phase 01. GitHub Actions로 backend(ktlint + test + build) + frontend(lint + test + build) 두 job을 PR/push 시 자동 실행. CHANGELOG와 HANDOFF를 phase 01 완료 상태로 갱신. phase 01의 마지막 step.

## 읽어야 할 파일

- `docs/ADR.md` ADR-015 (CI)
- `docs/CONTRIBUTING.md` (PR 체크리스트)
- `docs/HANDOFF.md` (갱신 대상)
- `CHANGELOG.md` (갱신 대상)
- `phases/01-project-skeleton/index.json` (전체 step 갱신 대상)

## 작업

### 6.1 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  backend:
    name: Backend (Kotlin + Spring Boot)
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}

      - name: ktlint
        run: ./gradlew :backend:ktlintCheck --no-daemon

      - name: Test
        run: ./gradlew :backend:test --no-daemon

      - name: Build
        run: ./gradlew :backend:build -x test --no-daemon

      - name: Upload test report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-report
          path: backend/build/reports/tests
          retention-days: 7

  frontend:
    name: Frontend (Vite + React)
    runs-on: ubuntu-latest
    timeout-minutes: 15
    defaults:
      run:
        working-directory: frontend
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - name: Install
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Typecheck
        run: npm run typecheck

      - name: Test
        run: npm run test -- --run

      - name: Build
        run: npm run build
```

> `package-lock.json`은 step 3의 `npm install` 결과로 생성된다. 커밋되어 있어야 cache hit. 없다면 step 3에서 `npm install` 대신 `npm install --package-lock-only` 후 커밋.

### 6.2 `.github/dependabot.yml` (선택 — ADR-015에 따라 주 1회)

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: "/"
    schedule:
      interval: weekly
    open-pull-requests-limit: 5

  - package-ecosystem: npm
    directory: "/frontend"
    schedule:
      interval: weekly
    open-pull-requests-limit: 5

  - package-ecosystem: docker
    directory: "/backend"
    schedule:
      interval: weekly

  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
```

### 6.3 `docs/HANDOFF.md` 갱신

`## 마지막 업데이트` 블록을 다음으로 교체 (날짜는 실행 시점으로):

```markdown
## 마지막 업데이트

- 시각: <YYYY-MM-DD> (phase 01 project-skeleton 완료)
- 직전 작업:
  - Gradle 8.7 + Kotlin 1.9.25 + Spring Boot 3.3.4 백엔드 골격
  - Vite 5 + React 18 + TS 5 + Tailwind 3 프론트엔드 골격
  - Docker Compose: postgres(pgvector) + backend + frontend-dev + caddy 4서비스 헬스 통과
  - GitHub Actions CI: backend(ktlint+test+build) + frontend(lint+typecheck+test+build)
  - 스모크 테스트: backend MnemeApplicationSmokeTest 2 케이스, frontend App 렌더 1 케이스
- 작성자 노트: phase 02 persistence-base 진입. Flyway V1__init.sql + JPA 엔티티 + pgvector 확장 활성화 + HikariCP + Testcontainers
```

`Phase 진행 상태` 표에서 `01 project-skeleton` 행 상태 → `completed`, 메모 → "compose 4서비스 헬스, CI 통과".

`다음 세션이 즉시 이어갈 작업` 섹션을 phase 02 작업으로 교체:

```markdown
### 우선순위 1: phase 02 설계 및 실행

1. `phases/02-persistence-base/index.json`과 stepN.md 작성
2. backend `build.gradle.kts`에 추가: spring-boot-starter-data-jpa, postgresql JDBC, flyway-core, com.pgvector(pgvector-java) 등
3. `application.yml`에서 DataSource autoconfigure exclude 두 줄 제거
4. `db/migration/V1__init.sql` 작성: users / api_keys / oauth_clients / oauth_tokens / sessions / folders / memories / tags / memory_tags / memory_links / memory_versions / audit_events / usage_daily + 인덱스 + pgvector 확장
5. 도메인 엔티티 + 리포지토리 (user_id 강제 시그니처)
6. Testcontainers + Postgres+pgvector 이미지로 통합 테스트 인프라
7. HikariCP 설정 적용
```

### 6.4 `CHANGELOG.md` 갱신

`## [Unreleased]` 의 `### Added` 아래에 추가:

```markdown
- Phase 01 project-skeleton:
  - Gradle 8.7 + Kotlin 1.9.25 + Spring Boot 3.3.4 백엔드 모듈
  - Vite 5 + React 18 + TypeScript 5 + Tailwind 3 프론트엔드 모듈
  - Docker Compose 스택: postgres(pgvector pg16) + backend + frontend-dev + caddy
  - GitHub Actions CI: ktlint, JUnit + Kotest, ESLint, Vitest, build 검증
  - 백엔드 스모크 테스트, 프론트엔드 App 렌더 테스트
```

`### 향후 마일스톤 (미릴리즈)` 의 `#### M1` 항목에 체크 추가:

```markdown
#### M1 — 로컬 부팅
- phase 01 ✅ / phase 02 (persistence-base) 진행 시 0.1.0 후보
```

### 6.5 `phases/01-project-skeleton/index.json` 최종 갱신

모든 step의 `status`를 `completed`로 변경하고 `summary` 한 줄을 추가. 예:

```json
{
  "project": "Mneme",
  "phase": "01-project-skeleton",
  "goal": "...",
  "milestone": "M1 — 로컬 부팅",
  "steps": [
    { "step": 1, "name": "gradle-root-and-backend-skeleton", "status": "completed",
      "summary": "gradle 8.7 + spring boot 3.3.4 + kotlin 1.9.25 백엔드 모듈, application.yml + ktlint 통과" },
    { "step": 2, "name": "backend-smoke-test", "status": "completed",
      "summary": "MnemeApplicationSmokeTest 2 케이스 PASSED (context load + /actuator/health UP)" },
    { "step": 3, "name": "frontend-vite-react-skeleton", "status": "completed",
      "summary": "vite 5 + react 18 + ts 5 + tailwind 3 + vitest 스모크 + ESLint --max-warnings 0 통과" },
    { "step": 4, "name": "docker-compose-stack", "status": "completed",
      "summary": "postgres(pgvector)+backend+frontend-dev+caddy compose + Caddyfile 검증" },
    { "step": 5, "name": "compose-health-verification", "status": "completed",
      "summary": "compose up 후 5종 헬스 검증(backend, caddy→backend, frontend, caddy→frontend, pg_isready) ALL OK" },
    { "step": 6, "name": "github-actions-ci-and-handoff", "status": "completed",
      "summary": "GitHub Actions backend+frontend 2 job + Dependabot + HANDOFF/CHANGELOG 갱신" }
  ]
}
```

### 6.6 커밋 (3개로 분할 권장 — CI / 문서)

```bash
# 1) CI
git add .github/
git commit -m "chore(ci): add github actions backend and frontend pipelines

- backend job: setup-java 21 + gradle cache + ktlintCheck + test + build
- frontend job: setup-node 20 + npm ci + lint + typecheck + test + build
- 실패 시 backend test 리포트 아티팩트 업로드
- concurrency cancel-in-progress 로 중복 실행 차단
- Dependabot: gradle/npm/docker/actions 주 1회

Refs: ADR-015"

# 2) 문서 + 진행 상태
git add docs/HANDOFF.md CHANGELOG.md phases/01-project-skeleton/index.json
git commit -m "docs: mark phase 01 complete and queue phase 02 persistence-base

- HANDOFF: 마지막 업데이트 + 다음 우선순위 phase 02로 교체
- CHANGELOG: M1 항목에 phase 01 완료 체크
- phases/01-project-skeleton/index.json: 6 step 모두 completed"
```

## Acceptance Criteria

```bash
# 워크플로 문법 검증 (actionlint 사용 가능 시)
docker run --rm -v "$PWD":/repo rhysd/actionlint:latest -color /repo/.github/workflows/ci.yml || true

# 백엔드/프론트 명령이 CI에서 호출하는 그대로 로컬에서 통과
./gradlew :backend:ktlintCheck :backend:test :backend:build
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run test -- --run
npm --prefix frontend run build
```

모두 exit 0. (actionlint는 권장이지만 미설치여도 무방.)

## 검증 절차

1. Acceptance Criteria 명령 모두 통과.
2. `docs/HANDOFF.md` 의 "Phase 진행 상태" 표에서 phase 01 행이 `completed`이고 phase 02 행이 `pending`인지 확인.
3. `CHANGELOG.md` 의 `## [Unreleased]` 에 phase 01 항목 + M1 체크 확인.
4. `phases/01-project-skeleton/index.json` 의 모든 step `status: completed` + `summary` 비어있지 않은지 확인.
5. (저장소가 git 원격에 연결되어 있다면) `git push` 후 GitHub Actions의 첫 실행 결과를 확인. 미연결이면 로컬 검증으로 충분.
6. 성공 시 phase 01 전체 종료. M1 마일스톤 달성 신호. Step 6 자체는 `completed`로 마킹.

## 금지사항

- CI 워크플로에 OpenAI/Google 키를 secret으로 추가하지 마라. **이유: phase 01 코드는 외부 호출 없음. 불필요한 비밀 노출**.
- `actions/cache@v4`를 수동으로 추가하지 마라. **이유: setup-gradle / setup-node가 캐싱 처리**.
- `git push --force` 또는 main 직접 push 금지. **이유: CLAUDE.md 금지 명령 + ADR-015 PR 게이트**.
- 워크플로에 임의 secret을 참조하지 마라. **이유: phase 01에서는 필요 secret 없음**.
- CHANGELOG에 phase 02 항목을 미리 적지 마라. **이유: 거짓 기록 — 완료 시점에만**.
