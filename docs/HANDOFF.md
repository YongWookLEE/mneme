# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-28 (phase 03 라이브 OAuth 검증 통과 + 부수 버그 보정)
- 직전 작업(라이브 검증 + 후속 보정):
  - Google OAuth Client 발급 후 `.env` 입력 → `http://localhost:8080/oauth2/authorization/google` → Google 로그인 → users 테이블에 sdsd1008@gmail.com 정상 upsert ✅
  - 발견·수정 버그 3건:
    1. V1__init.sql의 `memories` 테이블에 `version` 컬럼 누락 (Memory 엔티티의 @Version과 불일치) → 컬럼 추가
    2. kotlin("plugin.jpa") 누락 → JPA 엔티티가 no-arg 생성자 없어서 Hibernate InstantiationException → 플러그인 추가
    3. GlobalExceptionHandler가 NoResourceFoundException(404), ResponseStatusException(상태 코드 매핑)을 INTERNAL로 잘못 매핑 → 핸들러 추가
  - 본 phase에서는 V1을 직접 수정함(아직 어떤 환경에도 V1 적용된 적 없음 — 깨끗한 보정). 다음 phase부터는 ADR-014 forward-only 엄격 적용
- 이전 직전 작업(phase 05):
  - FolderService + FolderController(/api/folders): materialized path 자동 계산, 이름 중복 409, 자식 path 일괄 갱신은 phase 07 예약
  - MemoryService + MemoryController(/api/memories): create/get/update(낙관적 락 @Version, 409)/archive/restore/list, 본문 256KB 상한
  - TagService + TagController: getOrCreate(소문자 정규화 32자) + attach/detach(메모리당 16개) + /api/memories/{}/tags
  - AuthenticatedUserResolver: ApiKey 또는 OAuth2User principal → userId 통합 추출
  - IsolationRegressionTest 단위 베이스(folder/memory/tag service에서 다른 userId 접근 → 404)
- **사용자 액션 필요**:
  - phase 03 라이브 OAuth: 여전히 GCP credentials 필요
  - phase 06 라이브 LLM: OpenAI API 키 필요 (text-embedding-3-small + gpt-4o-mini, https://platform.openai.com/api-keys, .env의 MNEME_OPENAI_API_KEY)
- 작성자 노트: phase 06(llm-adapter)는 Spring AI + OpenAI 어댑터 + 프롬프트 인젝션 방어 + Caffeine 캐시. 코드는 자동 가능하지만 라이브 호출 검증은 키 필요. 그 전에 자동 진행

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
| 02 persistence-base | completed | Flyway V1(13 테이블+pgvector+pg_trgm), id 모듈(UUID v7+base32), 엔티티 7개, 리포지토리 7개. Testcontainers는 호환 이슈로 phase 05/08 보류 |
| 03 auth-google-oauth | completed (code + live ✅) | Spring Security + Google OAuth2 + 세션/CSRF/CORS/보안 헤더 + i18n 에러. 2026-06-28 라이브 검증 통과: sdsd1008@gmail.com → users row 정상 upsert. M2 마일스톤 도달 |
| 04 api-keys | completed | mn_ 키 발급/폐기/회전/이름수정 + Bearer 인증 필터 + AuditPublisher(key.created/revoked) |
| 05 memory-domain | completed | folder/memory/tag REST CRUD + 낙관적 락 + archive/restore + IsolationRegressionTest 베이스 |
| 06~15 | pending | ROADMAP 참조 (06은 LLM 어댑터 — OpenAI 키 라이브 검증 필요) |
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

### 우선순위 1 (사용자): phase 03 라이브 검증

GCP Console에서 OAuth Client 발급 → `.env`에 입력 → `docker compose up` 재기동 → `/oauth2/authorization/google` 호출.

### 우선순위 2 (코드): phase 04 api-keys

1. `phases/04-api-keys/index.json`과 stepN.md 작성
2. `mn_<32B base62>` 평문 키 발급기 + sha256 해시 + 앞 8자 prefix
3. `/api/keys` REST 컨트롤러: 발급/목록/폐기/이름 수정. 발급 응답에만 평문 1회 노출
4. API 키 인증 필터: `Authorization: Bearer mn_...` 헤더 → prefix 조회 → keyHash 일치 → SecurityContext 주입
5. 감사 이벤트 발행(key.created, key.revoked)
6. WebMvc 테스트 + AuthN/AuthZ 케이스

phase 04는 사용자 개입 없이 진행 가능. 본격 라이브 통합은 phase 15(client-validation)에서.
