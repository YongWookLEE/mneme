# Mneme Handoff

> 새 세션의 Claude/Codex는 이 파일을 가장 먼저 읽는다.
> 작업 단위가 끝나면 이 파일을 갱신하고 커밋한다.
> 전체 로드맵은 `docs/ROADMAP.md`.

## 마지막 업데이트

- 시각: 2026-06-27 (phase 02 persistence-base 완료)
- 직전 작업:
  - JPA + PostgreSQL JDBC + Flyway + pgvector-java + HikariCP 의존성 + application.yml DataSource/JPA/Flyway/Hikari 설정
  - Flyway V1__init.sql: 13개 테이블(users / api_keys / oauth_clients / oauth_tokens / sessions / folders / memories / tags / memory_tags / memory_links / memory_versions / audit_events / usage_daily) + vector/pg_trgm 확장 + ivfflat 임베딩 인덱스 + tsv 트리거. compose postgres 수동 검증 통과
  - id 모듈: UUID v7 단조 증가 생성기 + Crockford Base32(26자) + PrefixedId(11 prefix) + IdFactory. 단위 테스트 3개 클래스 PASSED
  - JPA 엔티티: User/ApiKey + Folder/Memory/MemoryLink/Tag/MemoryTag (Memory `@Version` 낙관적 락). embedding/tsv 매핑은 phase 06 예약
  - JPA 리포지토리 7개: 모든 조회 메서드 첫 인자 `userId: UUID` 강제(인증 단계의 `findAllByPrefixAndRevokedAtIsNull`만 예외, KDoc 명시). 본격 IsolationRegressionTest는 Testcontainers와 함께 phase 05/08 도입 예약
- 미해결 항목(phase 후순위 이관):
  - **Testcontainers 호환 이슈**: Docker Desktop 29.x + docker-java(testcontainers 1.20.2)에서 `DockerClientProviderStrategy`가 Info 응답 파싱 실패. phase 05/08에서 testcontainers 업그레이드와 함께 재시도
  - embedding/tsv/jsonb/inet/text[] 컬럼 JPA 매핑 — phase 05(memory-domain) / phase 06(llm-adapter)
- 작성자 노트: phase 03 auth-google-oauth 진입 — Google OAuth 코드는 작성 가능하지만 **end-to-end 검증은 사용자가 GCP Console에서 OAuth client 발급 + .env에 입력 후 가능**. 코드는 mock 기반 단위 테스트까지 자동 진행하고 그 직전에서 사용자 안내

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

### 우선순위 1: phase 03 auth-google-oauth 설계 및 실행

1. `phases/03-auth-google-oauth/index.json`과 stepN.md 작성
2. backend `build.gradle.kts`: spring-boot-starter-security, spring-boot-starter-oauth2-client
3. Spring Security 설정: Google OAuth2 로그인 + 세션 쿠키(Secure/HttpOnly/SameSite=Lax) + CSRF 토큰
4. `/oauth/google/callback` 처리: googleSub으로 User upsert → 세션 생성
5. CORS(MNEME_FRONTEND_ORIGIN 화이트리스트), 보안 헤더(CSP/HSTS/X-Content-Type-Options/Referrer-Policy/X-Frame-Options)
6. i18n 초기 셋업: 백엔드 에러 코드(ERR_*), 메시지 키 정의
7. 단위 테스트(Spring Security 설정 검증 + WebMvcTest mock OAuth2User)는 자동 가능
8. **사용자 개입 필요(blocked 가능)**: GCP Console에서 OAuth client 생성 → MNEME_GOOGLE_OAUTH_CLIENT_ID/SECRET을 `.env`에 입력. 이 단계 이전에 자동 진행 멈춤
