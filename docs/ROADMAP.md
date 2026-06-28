# Mneme Roadmap

> 전체 phase 흐름은 이 파일에서 관리한다. 지금 어디서 이어갈지는 `docs/HANDOFF.md`, 세부 실행 지시는 `phases/{phase-name}/index.json`과 `stepN.md`를 본다.

## 마일스톤

- **M1 — 로컬 부팅**: docker compose up으로 모든 컴포넌트가 뜨고 헬스 체크 통과 (phase 01~02)
- **M2 — 인증 완성**: 사용자가 Google 로그인 후 API 키를 발급/폐기 가능 (phase 03~04)
- **M3 — 메모리 도메인**: REST로 메모리 CRUD + 자동 분류·요약·임베딩·하이브리드 검색 (phase 05~07)
- **M4 — 보안 완전**: rate limit, 토큰 사용량, 격리 테스트, 보안 헤더 (phase 08)
- **M5 — MCP 라이브**: 11개 도구가 Claude.ai에서 실제로 호출됨 (phase 09~10)
- **M6 — 대시보드 사용 가능**: 폴더 트리·미리보기·편집·검색·온보딩·연결 가이드·archive·키 빌더 (phase 11~12)
- **M6.5 — 위키 경험**: 본문 `[[link]]` 파싱·backlink·`/map` 그래프 (phase 16~17)
- **M7 — 포터빌리티·운영**: export/import + 관측성 + 클라이언트 검증 (phase 13~15)
- **M8 — Wiki 확장**: lint, folder index, review feedback (phase 21~23)
- **M9 — 운영 안착**: 호스팅, 셀프호스팅 가이드, 백업, 사용량 알림 (phase 30~33)

## 범위 1: MVP (Heirmos 핵심 + 보안·관측·포터빌리티 기본)

| Phase | 상태 | 목적 | 메타 |
|-------|------|------|------|
| 00 docs-bootstrap | completed | PRD/ARCHITECTURE/ADR/ROADMAP/UI_GUIDE/DEVELOPMENT/SECURITY/CONTRIBUTING 채우기 + 라이선스·CHANGELOG | `phases/00-docs-bootstrap/` |
| 01 project-skeleton | pending | backend/frontend/deploy 골격 + Docker Compose 부팅 + CI 초기 | `phases/01-project-skeleton/` |
| 02 persistence-base | pending | Flyway 초기 마이그레이션(V1__init.sql) + JPA 엔티티 + pgvector 확장 활성화 + HikariCP 설정 + Testcontainers 셋업 | `phases/02-persistence-base/` |
| 03 auth-google-oauth | pending | Google OAuth + 세션 + CSRF + 보안 헤더 + i18n 초기화 | `phases/03-auth-google-oauth/` |
| 04 api-keys | pending | `mn_` 키 발급/해시/검증/폐기/회전 + 감사 로그 + 대시보드 키 UI | `phases/04-api-keys/` |
| 05 memory-domain | pending | memories/folders/tags REST CRUD + UUID v7/base32 ID + 사용자 격리 회귀 테스트 클래스 신설 | `phases/05-memory-domain/` |
| 06 llm-adapter | completed | OpenAI REST 어댑터(임베딩 1536d + gpt-4o-mini 요약/분류/태그) + PromptGuard 펜스 + Caffeine 캐시 + usage_daily 집계 + MemoryWriteFacade. ADR-020(Spring AI는 MCP 전용) | `phases/06-llm-adapter/` |
| 07 hybrid-search | completed | pgvector + tsvector + pg_trgm 결합 검색 + α/β/γ 가중치 + 필터(폴더/태그/날짜)·정렬. /api/search REST | `phases/07-hybrid-search/` |
| 08 security-controls | completed | rate limit(Caffeine 분/일/쓰기) + 일일 LLM/embed 토큰 한도 가드 + PII 로그 마스킹 + IsolationRegressionTest 확장. M4 도달 | `phases/08-security-controls/` |
| 09 mcp-server | completed | Spring AI MCP server starter(WebMVC) + 11개 `mn_*` `@Tool` + Reactor 컨텍스트 자동 전파(Micrometer + Hooks)로 SecurityContextHolder가 boundedElastic 워커까지 전파. 라이브: tools/list 11개 + mn_whoami/mn_list/mn_search 정상 | `phases/09-mcp-server/` |
| 10 mcp-oauth-dcr | completed | RFC 7591 DCR(`/oauth/register`) + Authorization Code(PKCE S256) + refresh + OAuth access token Bearer 인증 필터. 라이브: DCR/refresh/whoami 정상. **M5 도달**. authorize 종단 라이브는 phase 15 | `phases/10-mcp-oauth-dcr/` |
| 11 dashboard-ui | completed | 폴더 트리·메모리 리스트·상세·마크다운 편집(낙관적 락 충돌 UI)·`/search`·`/archive`·`/keys`(발급/폐기/회전 + MCP 명령 빌더). Mod+K 단축키. ApiKeyController Bearer 통합 + V3 audit_events.ip TEXT 보정 | `phases/11-dashboard-ui/` |
| 12 onboarding-guide | completed | 첫 로그인 4단계 `OnboardingTour` + `/connect` 클라이언트 가이드 페이지(Claude/Codex/ChatGPT 스니펫 + 복사 + 스크린샷 자리). **M6 달성** | `phases/12-onboarding-guide/` |
| 13 export-import | completed | `ExportService` zip 스트리밍(manifest.json + memories/*.md frontmatter) + `ImportService` 2단계 흐름(preview/apply Caffeine 30분 TTL + skip/replace/create-new) + 프론트 `/data` 페이지(다운로드 + 업로드 + 항목별 충돌 결정) | `phases/13-export-import/` |
| 14 observability | pending | Prometheus 메트릭, 구조화 로깅, 감사 이벤트 사용자 조회 UI, 사용량 대시보드 | `phases/14-observability/` |
| 15 client-validation | pending | Claude.ai / ChatGPT Developer mode / Codex CLI 실연결 + 격리 회귀 종합 실행 | `phases/15-client-validation/` |
| 16 wiki-link-parser | pending | 본문 `[[wiki-link]]` 파서 + `memory_links` 동기 인덱스 + 제목 변경 시 backlink 본문 일괄 치환 + `mn_relations` 도구 구현 + 시스템 프롬프트에 `[[link]]` 삽입 가이드 추가 | `phases/16-wiki-link-parser/` |
| 17 memory-map-ui | pending | `/map` 페이지: 그래프 라이브러리 결정(ADR), force-directed 레이아웃, 노드 호버 카드, 깨진 링크 표시, 필터, `Cmd/Ctrl+G` 단축키, 편집기 `[[` 자동완성, backlink 패널 | `phases/17-memory-map-ui/` |

## 범위 2: Wiki 확장

| Phase | 상태 | 목적 | 메모 |
|-------|------|------|------|
| 21 folder-index | deferred | 폴더별 `index.md` LLM 자동 생성·유지 (배치 잡) | |
| 22 lint-tools | deferred | 모순·고립·누락 감지, 대시보드 검토 패널 | |
| 23 review-feedback | deferred | 사용자 피드백(맞아요/틀렸어요) → 분류 가중치 조정 | |

> phase 20 wiki-links는 MVP phase 16으로 승격(2026-06-27, ADR-018).

## 범위 3: 운영·배포·확장

| Phase | 상태 | 목적 | 메모 |
|-------|------|------|------|
| 30 hosting-decision | deferred | VPS/AWS/Fly.io 실제 호스팅 선택 + 도메인 + Caddy 설정 | MVP 동작 후 |
| 31 selfhost-guide | deferred | 다른 사람용 README + OAuth 등록 스크린샷 + 트러블슈팅 | |
| 32 backup-restore | deferred | pg_dump cron + B2/S3 업로드 + restore 스크립트 + 정기 복구 리허설 | |
| 33 usage-quotas-alerts | deferred | 사용자별 토큰 사용량 대시보드 + 이메일 알림 + 관리자 메트릭 | |
| 34 at-rest-encryption | deferred | 메모리 본문 컬럼 envelope encryption 옵션 | 민감 데이터 사용자 대비 |
| 35 attachments | deferred | 이미지·파일 업로드 (외부 객체 스토리지) | |
| 36 sharing-readonly | deferred | 메모리/폴더 읽기 전용 공유 링크 | |
| 37 more-oauth-providers | deferred | GitHub OAuth 등 추가 제공자 | |
| 38 mobile-pwa | deferred | 모바일 PWA(검색·읽기·간단 작성) | |

## 미확정 결정

- 실제 호스팅 위치 (Hetzner / AWS / 자가 서버)
- 도메인 이름 (mneme.app / mneme.dev / mneme.kr 등)
- 이메일 발송 제공자 (SendGrid / Amazon SES / Resend)
- 알림 채널 확장 시 우선순위 (이메일 → Slack/Discord/Webhook)

## 로드맵 갱신 원칙

- phase가 완료되면 본 표에 상태와 한 줄 summary를 반영한다.
- 새 phase를 시작하기 전에 `phases/{phase-name}/` 디렉터리, `index.json`, `stepN.md`를 작성한다.
- 기술 선택이 바뀌면 `docs/ADR.md`에 결정 근거를 먼저 남긴다.
- `docs/ROADMAP.md`와 `docs/HANDOFF.md`는 짝이다. ROADMAP은 전체 그림, HANDOFF는 다음 세션이 바로 이어갈 위치.
- 마일스톤 도달 시 `CHANGELOG.md`에 항목 추가.
