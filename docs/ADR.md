# Architecture Decision Records

이 파일은 중요한 기술 결정을 기록한다. 각 결정은 무엇을 선택했는지, 왜 선택했는지, 무엇을 포기했는지를 포함한다.

## ADR-001: 백엔드 스택은 Kotlin + Spring Boot 3.x

**상태**: accepted

**결정**: 백엔드를 Kotlin + Spring Boot 3.x로 구현한다.

**이유**: 작성자(주 사용자이자 유일한 유지보수자)가 Java/Kotlin에 익숙하다. ~10명 규모에서 JVM의 "무거움"은 실질 문제가 아니다. Spring Security OAuth2, Spring AI(MCP/임베딩), Spring Data JPA를 한 생태계에서 받아 자체 구현 비용을 최소화한다.

**트레이드오프**: 콜드 스타트 5~15초, 기본 메모리 300~500MB. 단일 VPS 상시 가동 모델에선 무관하지만 서버리스 배포는 비효율.

**대안**:
- TypeScript + Hono: MCP 공식 SDK 1등이지만 학습 비용 + 인증/ORM 성숙도 떨어짐
- Python + FastAPI: AI 친화적이지만 작성자 미숙
- Go: 가볍지만 학습 비용 + MCP SDK 미성숙

## ADR-002: 데이터 저장소는 PostgreSQL + pgvector 단일 DB

**상태**: accepted

**결정**: 메타데이터·전문검색·벡터 모두 PostgreSQL 한 곳에 두고 pgvector 확장으로 벡터 검색한다.

**이유**: 10명 규모에서 별도 벡터DB 도입은 운영 복잡도 대비 이점이 없다. 단일 트랜잭션으로 메모리 메타와 임베딩을 함께 저장/삭제할 수 있어 정합성 단순.

**트레이드오프**: 벡터 검색 성능은 전용 DB보다 낮음. 수십만 메모리 규모까진 무관, 그 이상이면 마이그레이션 필요.

**대안**:
- Postgres + Qdrant 분리: 성능 우위 있으나 10명 규모엔 과잉
- SQLite + sqlite-vec: 셀프호스팅 친화적이지만 다중 사용자 동시 쓰기/전문검색 제약

## ADR-003: 정체성은 Heirmos 골격 + LLM Wiki 본문 링크(MVP 핵심)

**상태**: accepted (2026-06-27 개정 — ADR-018 참조)

**결정**: 인프라 골격(MCP 도구·폴더 브라우저·하이브리드 검색·OAuth·API 키·격리)은 Heirmos를 따른다. 본문 안 `[[wiki-link]]` 파싱과 `/map`(관계 그래프 뷰)을 **MVP 핵심**으로 포함해 LLM Wiki/옵시디언적 경험을 1차 정체성으로 노출한다. `index.md` 자동 유지·lint 도구·LLM 능동 합성 잡 등 보조 기능은 후속.

**이유**: 사용자 검증 결과(2026-06-27) "옵시디언이 알아서 잘 해주던데" — 본문 markdown에 LLM이 `[[link]]`를 자연 삽입하는 경험을 1차 사용성으로 채택. 별도 관계 UI는 매끄러움이 떨어지고, Heirmos와의 차별화도 약하다. `[[link]]` 파싱 비용은 작고, MCP 도구는 본문이 진실인 구조와 호환된다.

**트레이드오프**: phase 20에 있던 wiki-links 작업이 MVP 핵심 경로로 들어와 일정 압박 증가. 응답 지연은 본문 파싱이 가벼워 무관, LLM이 작성 시 `mn_search`를 한 번 더 호출하는 비용만 발생.

**대안**:
- 순수 Heirmos 클론: 차별화 약함
- 별도 관계 테이블 + 수동 연결 UI: UX 매끄러움 떨어짐, LLM Wiki 경험과 어긋남
- 풀 LLM 능동 합성(`index.md` 자동 유지·일괄 lint): 응답 시간·토큰 비용 부담, MVP 후속으로 분리

## ADR-004: LLM/임베딩은 OpenAI 단일 제공자로 시작

**상태**: accepted

**결정**: 임베딩은 `text-embedding-3-small`(1536차원), 자동 분류/요약은 `gpt-4o-mini`. Spring AI 추상화로 후속 교체 가능.

**이유**: API 키 1개로 운영 단순. 두 모델 모두 한국어 양호, 가격 저렴. Spring AI 표준 인터페이스로 향후 교체 시 어댑터만 변경.

**트레이드오프**: OpenAI 장애·정책 변경 시 직접 영향. 한국어 미세 뉘앙스는 Claude Haiku보다 약간 떨어질 수 있음.

**대안**:
- 하이브리드(OpenAI 임베딩 + Claude Haiku 분류): 품질 우위지만 키 2개·비용 증가
- Gemini 단일: 거의 무료지만 레이트 리밋·안정성 우려

## ADR-005: 인증은 Google OAuth + `mn_` API 키

**상태**: accepted

**결정**: 1차 로그인은 Google OAuth만. 사용자별 API 키는 `mn_<32B base62>`, sha256 해시만 DB 저장. MCP 클라이언트는 동적 클라이언트 등록(DCR, RFC 7591) 지원.

**이유**: 단일 OAuth 제공자로 시작해 구현 부담 최소화. `mn_` prefix는 Mneme 식별성. sha256은 고엔트로피 토큰에 충분, 검증 빠름. DCR이 있어야 Claude.ai·ChatGPT가 client_id 사전 등록 없이 연결됨.

**트레이드오프**: Google 계정 없는 사용자는 가입 불가(GitHub OAuth 등은 후속).

**대안**:
- bcrypt 해시: 무차별 대입 방어력 더 높지만 검증 느림. 32B 랜덤 키엔 과잉
- 비밀번호 로그인: 비밀번호 관리·재설정 비용

## ADR-006: MCP 서버는 Spring AI MCP Server starter

**상태**: accepted

**결정**: `spring-ai-mcp-server-webmvc` starter로 MCP 서버를 띄우고, 11개 도구는 Kotlin 메서드에 `@Tool`로 노출. 전송은 Streamable HTTP.

**이유**: Spring Boot 통합이 자연스러워 인증·DI·로깅·관측을 표준 방식으로 처리. 도구 이름은 `mn_` prefix로 다른 MCP 서버와 충돌 회피.

**트레이드오프**: Spring AI MCP starter는 신생이라 API 변경 가능성 있음.

**대안**:
- MCP Kotlin SDK 직접 사용: Spring 통합 코드 직접 작성 필요
- MCP 프로토콜 직접 구현: 학습 가치는 있으나 시간 소모 큼

## ADR-007: 프론트엔드는 React + Vite + shadcn/ui

**상태**: accepted

**결정**: 대시보드는 React + Vite SPA. UI는 shadcn/ui, 서버 상태는 TanStack Query. 빌드 산출물은 백엔드 `static/`에 복사해 단일 배포.

**이유**: 폴더 트리 ↔ 미리보기 ↔ 검색의 활발한 상호작용은 SPA가 자연스럽다. shadcn/ui로 다크·미니멀 톤 빠른 구현. 작성자 학습 부담은 AI 도구 도움으로 보완.

**트레이드오프**: 백엔드와 별개의 빌드/의존성 관리(npm), JS/TS 학습 필요.

**대안**:
- Thymeleaf + HTMX: Spring 단일 빌드지만 폴더 브라우저 UX 매끄러움 떨어짐
- Vue 3: 학습 곡선 낮으나 생태계는 React보다 작음

## ADR-008: 개발/배포 단위는 Docker Compose

**상태**: accepted

**결정**: 백엔드·Postgres·Caddy·프론트엔드 dev 서버를 `docker-compose.yml` 하나로. 로컬/운영 동일 파일, 차이는 `.env`와 `docker-compose.override.yml`로만.

**이유**: "다른 사람도 셀프호스팅 가능"이 목표. Compose 파일이 그대로 셀프호스팅 가이드. 호스팅 사업자 종속 없음.

**트레이드오프**: Kubernetes 같은 본격 오케스트레이션 불가. 10명 규모엔 무관.

**대안**:
- 직접 systemd: 이식성 떨어짐
- Kubernetes: 명백한 오버킬

## ADR-009: 사용자 데이터 격리는 코드 + 테스트 이중 강제

**상태**: accepted

**결정**: 모든 리포지토리 메서드는 user_id를 첫 인자로 명시. 자동 통합 테스트가 "사용자 A → 사용자 B 데이터 접근 → 404" 시나리오를 모든 11개 MCP 도구·REST 엔드포인트에 대해 검증. PostgreSQL Row-Level Security는 도입하지 않는다.

**이유**: 멀티테넌트의 가장 큰 사고는 사용자 데이터 교차 노출. 코드 규약만으로는 회귀 위험. 권한 위반 시 404를 반환해 리소스 존재 여부도 비노출.

**트레이드오프**: 모든 메서드에 user_id 반복 전달로 boilerplate 증가. `@TenantScoped` AOP 추상화로 일부 완화 가능하지만 명시성 우선.

**대안**:
- Postgres RLS: DB 레벨 강제지만 마이그레이션·커넥션 풀 정책 복잡
- ThreadLocal 컨텍스트: 비동기 코드에서 손실 위험

## ADR-010: 보안 정책 (rate limit, 비용 통제, 헤더, 로깅)

**상태**: accepted

**결정**:
- Rate limit: 사용자별 분당 60회/일별 5,000회, `mn_write`는 분당 20회. in-memory token bucket으로 시작, 수평 확장 시 Redis 전환
- 토큰 사용량: 일일 임베딩 100K + LLM 50K 한도. 초과 시 호출 차단 + 이메일 알림
- 보안 헤더: CSP, HSTS, X-Content-Type-Options, X-Frame-Options:DENY, Referrer-Policy:strict-origin-when-cross-origin
- 로깅 마스킹: Authorization 헤더, Set-Cookie, 메모리 본문, 임베딩 자동 마스킹

**이유**: 키 도용 한 건이 OpenAI 청구서를 폭주시킬 수 있다. 한도는 일반 사용 대비 충분히 여유 있게(분당 60회 ≈ 초당 1회 평균) 두되 명시적 상한 필요.

**트레이드오프**: in-memory bucket은 서버 재시작 시 리셋 → 잠시 한도 우회 가능. 수평 확장 시 Redis로 옮겨야 함.

**대안**:
- 외부 API 게이트웨이(Kong 등): 오버킬
- 한도 없음: 사고 시 직접 비용 부담

## ADR-011: 데이터 포터빌리티·백업·import

**상태**: accepted

**결정**: 사용자는 언제든 자기 데이터를 zip(마크다운 + manifest.json)으로 다운로드. 같은 형식의 zip을 import할 수 있고, 일반 마크다운 zip도 폴더 구조 보존하며 import. 서버는 일 1회 pg_dump를 외부 객체 스토리지에 업로드(일 7 + 주 4 + 월 12). 복구는 `deploy/scripts/restore.sh`로 단일 명령.

**이유**: 사용자가 자기 데이터에서 빠져나올 수 있어야 신뢰가 형성된다. import 통로가 없으면 export는 반쪽. 백업은 단일 VPS 장애·디스크 손상 시 유일한 안전망.

**트레이드오프**: import 시 ID 충돌·중복·임베딩 재계산 등 정책 복잡. 사용자 선택 UI 필요.

**대안**:
- export 미제공: 사용자 락인. 신뢰 저하
- 실시간 스트리밍 백업(WAL-G): 운영 복잡, 10명 규모엔 과잉

## ADR-012: 메모리 ID는 UUID v7 + base32 외부 노출

**상태**: accepted

**결정**: 모든 엔티티 PK는 UUID v7. 외부 노출 시 prefix + base32(소문자) — 예: `mem_2v8gqj4z7n3kh5p9r1t6w0xy2a`. API/MCP는 외부 ID만 받음.

**이유**: UUID v7은 시간순 정렬 가능해 인덱스 효율과 정렬 자연스러움 모두 확보. base32(crockford 변형)는 대소문자 무관·혼동 문자 제외·URL 안전. prefix가 ID 타입을 명시해 디버깅·로그 가독성 향상.

**트레이드오프**: 외부↔내부 변환 비용 발생(요청당 1~2회). 단순 자연수 ID보다 길어 사용자 가독성 떨어짐.

**대안**:
- 단순 자동 증가 ID: 사용자 수·메모리 수가 외부에 노출됨
- nanoid: 시간순 정렬 불가
- ULID: UUID v7과 거의 동등하지만 표준화는 UUID v7이 우세

## ADR-013: 폴더는 1:N, 태그는 N:M

**상태**: accepted

**결정**: 메모리는 정확히 한 폴더에 속한다(파일 시스템 비유). 태그는 다중 부여 가능. 폴더는 계층 구조(materialized path), 태그는 평면.

**이유**: 폴더 단일 소속은 "어디 있는지" 직관적으로 정의해주고, 사용자가 LLM의 자동 분류 결과를 검토·수정하기 쉽다. 태그는 다중 분류·횡단 검색에 적합. 둘 다 제공해 사용자 워크플로우 폭 확장.

**트레이드오프**: 모델 복잡도 증가. 자동 분류 시 폴더와 태그를 동시에 추론해야 함.

**대안**:
- 폴더만: 단일 차원 분류로 부족
- 태그만(Gmail식): 검색은 좋지만 "어디 있는지" 직관 부족, 사용자가 익숙한 폴더 메타포 상실
- 폴더 다중 소속: 동기화·중복 관리 복잡

## ADR-014: Flyway 마이그레이션은 forward-only

**상태**: accepted

**결정**: 모든 DB 마이그레이션은 `V{버전}__{설명}.sql` 형식, forward-only. rollback 마이그레이션 작성 금지. 잘못된 마이그레이션은 새 버전으로 보정.

**이유**: rollback 스크립트는 작성·테스트 비용이 크고, 운영 중 rollback은 데이터 손실 위험. forward-only로 두면 "현재 버전이 곧 진실"이고 분기 없음. 컬럼 추가는 nullable → 백필 → NOT NULL 3단계로 안전 변경.

**트레이드오프**: 실수한 마이그레이션을 즉시 되돌릴 수 없음. 보정 마이그레이션 작성·테스트 필요.

**대안**:
- Liquibase rollback: 도구 학습 비용 + 운영 위험
- 수동 DDL: 추적 불가, 환경 간 불일치

## ADR-015: CI는 GitHub Actions, 배포는 수동 승인

**상태**: accepted

**결정**: GitHub Actions로 PR 시 lint + 단위 + 통합 테스트 실행. main 푸시 시 Docker 이미지 빌드 + GHCR 푸시. 운영 배포는 manual approve(릴리즈 만들 때 수동 트리거).

**이유**: 10명 규모·1인 유지보수에선 자동 배포의 위험(잘못된 PR이 즉시 운영 영향)이 자동화 가치보다 큼. GitHub Actions는 무료 한도가 충분하고 마켓플레이스가 풍부.

**트레이드오프**: 배포 시 수동 클릭 필요. 자동 카나리·블루그린 없음.

**대안**:
- 자동 배포: 회귀 위험
- CircleCI/Jenkins: 별도 인프라 운영

## ADR-016: 동시 편집은 낙관적 락 + 충돌 시 사용자 병합

**상태**: accepted

**결정**: 메모리 수정은 클라이언트가 `updated_at`(또는 `version`)을 함께 전송. 서버는 일치하지 않으면 409 + 두 버전 본문 반환. 프론트엔드는 diff 표시 후 사용자 선택.

**이유**: 메모리는 사용자 1인이 주로 편집하는 자산이라 충돌 빈도 낮음. 낙관적 락이 가장 단순. 실시간 협업(CRDT 등)은 MVP 범위 초과.

**트레이드오프**: 충돌 시 사용자 개입 필요. 자동 병합 없음.

**대안**:
- 비관적 락: 편집 중인 메모리 잠금. 의도하지 않은 잠금 잔류 위험
- CRDT: 실시간 협업 가능하지만 구현 복잡

## ADR-017: i18n은 백엔드 코드 + 프론트엔드 번역 분리

**상태**: accepted

**결정**: 백엔드 에러는 영어 코드(`ERR_*`)와 i18n 키만 응답. 사람이 읽을 메시지는 프론트엔드 `react-i18next`에서 번역. MVP는 한국어만 채움, 영어 키는 미래용.

**이유**: 다국어 지원 시 백엔드 응답을 손대지 않고 번역만 추가하면 됨. 에러 코드는 모니터링/로그 분석에서 언어 무관 키로 동작.

**트레이드오프**: 신규 에러 추가 시 코드·키·번역 3곳 갱신 필요.

**대안**:
- 백엔드에서 직접 번역: Accept-Language 처리 복잡, 캐싱 무효화 어려움
- 영어 고정: 사용자 친화성 떨어짐

## ADR-018: 관계는 본문 `[[wiki-link]]`가 진실, `memory_links`는 파생 인덱스

**상태**: accepted (2026-06-27)

**결정**: 메모리 간 관계는 본문 마크다운 안의 `[[메모리 제목]]` 또는 `[[mem_<base32>]]` 표기로 표현한다. 백엔드는 메모리 저장 시 본문을 파싱해 `memory_links(source_id, target_id, kind, position)` 인덱스를 양방향(forward + backlink) 갱신한다. 별도의 관계 CUD MCP 도구는 만들지 않는다 — `mn_write`/`mn_update`로 본문을 바꾸면 인덱스가 자동 따라간다. `mn_relations`는 인덱스 조회 + 이웃 탐색 전용.

**이유**: LLM Wiki/옵시디언 사용 경험상 "본문 = 관계"가 가장 자연스럽다. 별도 관계 모델은 본문과 동기화 부담을 만들고, LLM이 `mn_write` 외에 별도 `mn_relate`를 호출해야 하는 추가 단계가 생긴다. 본문 단일 진실은 export(`zip + manifest.json`)도 단순화한다 — 본문 markdown만 빼면 관계까지 따라옴.

**트레이드오프**:
- 깨진 링크(`[[없는 제목]]`) 발생 가능. 표시는 하되 자동 생성/삭제 금지.
- 메모리 제목 변경 시 모든 backlink 본문을 검색·갱신해야 함. 트랜잭션 + 대상 N개 일괄 처리(폴더 이동과 같은 패턴).
- 같은 제목 중복 시 모호. 제목 충돌 시 `[[mem_<id>]]` 형식 권장 + 작성 시 LLM 가이드.

**대안**:
- 별도 `memory_relations` 테이블 + 수동 연결 UI: UX 매끄러움 떨어짐, 본문↔관계 이중 진실 위험
- LLM 자동 관계 추론 백그라운드 잡: 응답 시간·비용 증가, "스스로 업데이트되는 거 싫다"는 사용자 정책 위반
- 임베딩 유사도만으로 그래프: 사용자 의도와 무관한 노이즈 다수

## ADR-019: 관계 그래프 UI는 `/map`, Heirmos "성좌" 명명은 채택하지 않음

**상태**: accepted (2026-06-27)

**결정**: 본문 `[[link]]` 그래프 시각화 페이지를 `/map`으로 노출한다. 메뉴 라벨은 "맵", 영문 "Memory Map". Heirmos의 "성좌·별·지형·물길·풍화·기반암" 같은 시적 명명은 채택하지 않는다. 그래프 라이브러리는 phase 진입 시 react-force-graph / Cytoscape.js / sigma 중 벤치마크 후 결정 — ADR 분리.

**이유**: Mneme 톤은 "차분하고 도구적"(UI_GUIDE.md). 시적 명명은 톤과 어긋난다. `/map`은 옵시디언 graph view의 정신적 등가물로, 사용자가 즉시 의미를 파악할 수 있는 도구적 명칭.

**트레이드오프**: 감성적 차별화 포기. 단, [[wiki-link]] 본문 파싱이라는 기능적 차별화가 더 강하므로 톤 일관성 우선.

**대안**:
- 성좌/Constellation: Heirmos 그대로, 차별화 약함, 톤 불일치
- Graph/그래프: 너무 기술적, 비개발 사용자에게 차가움
- Network/기억망: 시각적이지만 도구성 약함

## ADR-020: LLM/임베딩 호출은 OpenAI REST API 직접 호출, Spring AI는 MCP 서버에만 사용

**상태**: accepted (2026-06-28)

**결정**: phase 06 임베딩/요약/분류 호출은 Spring `RestClient`로 OpenAI HTTP API(`/v1/embeddings`, `/v1/chat/completions`)를 직접 호출한다. Spring AI 의존성은 phase 09(MCP 서버)의 `spring-ai-starter-mcp-server`에만 도입한다.

**이유**:
- MVP에서 임베딩·채팅은 2개 엔드포인트만 쓴다. 추상화 이득보다 의존성 트리·버전 호환 비용이 크다.
- Spring AI 1.0.0 GA는 모델·옵션 API가 빠르게 진화 중. 향후 업그레이드 충격을 MCP 영역에만 가둔다.
- 프롬프트 인젝션 방어·캐시·로그 마스킹·토큰 사용량 집계를 RestClient 래퍼 한 곳에 모으는 편이 추적하기 쉽다.

**트레이드오프**: OpenAI 외 모델(Anthropic/local)을 추가하려면 어댑터 직접 작성. 단 ADR-004가 OpenAI 단일을 못박았으므로 실질 비용 0.

**대안**:
- `spring-ai-starter-model-openai`: 자동 설정 편하지만 의존성·옵션 매핑 학습 비용.
- LangChain4j: 추상화 풍부하나 ADR-001 Spring 생태계 단일화 원칙과 어긋남.

## ADR-021: MCP 도구 실행 시 SecurityContext를 Micrometer ContextPropagation으로 전파

**상태**: accepted (2026-06-28)

**결정**: Spring AI MCP server가 Reactor `boundedElastic` 스케줄러로 도구를 dispatch 하면서 servlet 스레드의 `SecurityContextHolder`(ThreadLocal)가 도구 메서드에서 사라지는 문제를 `McpContextPropagationConfig`로 해결한다. Micrometer Context Propagation의 `ThreadLocalAccessor`에 SecurityContext를 등록하고 `Hooks.enableAutomaticContextPropagation()`을 켠다.

**이유**: 도구 메서드가 `AuthenticatedUserResolver.currentUserId()`를 호출해 격리·인가를 일관되게 적용하려면 ThreadLocal이 그대로 보여야 한다. Reactor Context 사용으로 전면 재설계하는 비용 대신 Micrometer 표준 통합을 사용.

**트레이드오프**: 의존성에 `io.micrometer:context-propagation` 1.1.x가 필요(Spring Boot 3 stack에 이미 포함). 다른 ThreadLocal(RequestContextHolder 등)도 자동으로 전파되므로 의도하지 않은 부작용 가능 — 도구 단위 격리 테스트로 회귀 방지.

**대안**:
- ToolContext 파라미터로 sessionId를 받아 별도 sessionId→userId 맵 lookup: MCP SDK 변경에 취약.
- `MODE_INHERITABLETHREADLOCAL` SecurityContextHolder 전략: 스레드 풀 재사용 환경에서 신뢰 어려움.

## ADR-022: 본문 [[link]] 인덱스 동기 갱신 + 별도 빈으로 backlink rename

**상태**: accepted (2026-06-28)

**결정**: `MemoryWriteFacade`가 메모리 create/update 시 `WikiLinkIndexer.reindex(userId, sourceId, content)`로 `memory_links` 테이블을 source 단위로 비우고 재삽입한다. 제목이 바뀌면 별도 빈 `BacklinkRenameService.rename(userId, oldTitle, newTitle, except)`을 호출해 다른 메모리 본문의 `[[oldTitle]]`을 `[[newTitle]]`로 일괄 치환하고 인덱스를 다시 만든다.

**이유**: 본문이 관계의 진실(ADR-018)이므로 인덱스를 lazy로 두면 mn_relations·/map이 stale을 보여 준다. 동기 갱신은 ~10명·~수천 메모리 규모에서 충분히 빠르고 결과가 곧 보이는 UX 이득이 크다. `@Transactional` 메서드를 같은 클래스에서 호출하면 Spring AOP 프록시가 적용되지 않으므로 backlink rename은 별도 빈으로 분리해야 트랜잭션이 유효하게 동작한다.

**트레이드오프**: 메모리 수가 만 건을 넘으면 rename cascade는 비용이 증가. 그 시점에 비동기 잡으로 분리한다(미래 phase).

**대안**:
- LISTEN/NOTIFY 기반 비동기 reindex: 즉시 표시 안 되는 UX 후퇴.
- 본문 안에 `<a data-target="…">` 등 별도 마크업: 마크다운의 휴대성·옵시디언 호환 손실.

## ADR-023: 폴더 인덱스는 LLM이 합성한 정적 스냅샷

**상태**: accepted (2026-06-28)

**결정**: 폴더마다 `FolderIndex` 테이블 한 행을 두고, 사용자가 명시적으로 "재생성"을 누르면 그 폴더 안 메모리 목록(제목+요약)을 `ChatService.synthesizeFolderIndex`에 전달해 마크다운 한 페이지를 받는다. 자동 백그라운드 잡은 두지 않는다.

**이유**: 메모리 추가·수정마다 LLM을 자동 호출하면 토큰 비용이 폭주한다. 폴더 단위 요약은 "지금 한번 봐 두자" 정도의 빈도라 사용자 트리거가 적절하다. 합성 결과는 시스템 프롬프트(`folder-index.md`)로 주제별 그루핑 + `[[wiki-link]]` 자동 삽입 + 빈 곳 추측까지 일관되게 받는다.

**트레이드오프**: 사용자가 누르지 않으면 stale. 추후 폴더 변경 횟수 임계치 기반 자동 재생성 옵션을 둘 수 있다.

**대안**:
- 폴더 변경 트리거: 토큰 폭주.
- 실시간 합성(요청 시점에 매번 LLM 호출): 응답 지연 + 비용.

## ADR-024: lint 룰 4종, 사용자 검토 워크플로 우선

**상태**: accepted (2026-06-28)

**결정**: `LintService.runAll`이 다음 4룰을 본인 활성 메모리에서 감지한다 — `broken`(target_id=null), `orphan`(source/target 어디에도 안 잡힘), `stub`(byteSize<120B), `dup-title`(같은 폴더 동일 제목 소문자 비교). 결과는 카테고리별 카운트 + 이슈 리스트로 반환. 자동 수정은 하지 않는다.

**이유**: 자동 수정은 신뢰 가능한 룰만 가능한데, "외톨이"·"미완성" 같은 룰은 사용자 의도가 들어가야 한다. lint는 "여기 봐 주세요" 신호만 주고 수정은 사용자가 직접 한다.

**트레이드오프**: 감지하지 못하는 약점(모순된 진술, 잘못된 분류 등)은 별도 phase. 룰 추가는 인터페이스 안정성 위해 같은 4룰 안에 detail 메시지로 확장.

**대안**: LLM 기반 약점 감지 — 비용·일관성 문제로 후속.

## ADR-025: 사용자 피드백은 시스템 프롬프트 후미에 자동 inject

**상태**: accepted (2026-06-28)

**결정**: `MemoryFeedback` 테이블에 사용자가 남긴 👍/👎 + 노트를 저장하고, `FeedbackHintBuilder`가 `ChatService.call` 호출 직전 시스템 프롬프트 후미에 최근 8건(부정 우선 정렬)을 자동으로 append한다. 별도 학습 잡이나 가중치 모델은 두지 않는다.

**이유**: ~10명 규모에서 별도 ML 파이프라인은 비용·복잡도가 과하다. 프롬프트 컨텍스트로 최근 신호를 주는 것만으로도 분류·요약·태그 정확도가 개선된다. 즉시 반영되고, 피드백을 지우면 다음 호출부터 영향이 사라져 거버넌스도 단순하다.

**트레이드오프**: 8건 ≈ 200~500 토큰이 시스템 프롬프트에 추가되어 비용 약간 증가. 부정 우선 정렬이 긍정 신호를 묻히게 할 수 있어 향후 가중치 튜닝 여지.

**대안**:
- 별도 fine-tune 또는 LoRA: 비용·복잡도·재현성 모두 무거움.
- 임베딩 기반 user preference 모델: 데이터 부족 단계에서 의미 약함.
