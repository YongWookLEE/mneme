# PRD: Mneme

> Mneme — 그리스 신화의 기억의 뮤즈. 여러 AI 클라이언트가 공유하는 단일 영구 기억층.

## 목표

여러 AI 클라이언트(Claude, ChatGPT, Gemini, Codex, Grok 등)가 하나의 영구 메모리 저장소를 공유하도록 하는 셀프호스팅 가능한 멀티테넌트 인프라를 구축한다. AI 도구 간 컨텍스트 단절을 제거하고, 사용자가 자기 데이터의 완전한 소유권·이동권을 갖도록 한다.

## 사용자

| 군 | 규모 | 핵심 욕구 |
|---|---|---|
| 본인 (1차) | 1 | 모든 AI 도구가 동일한 컨텍스트(개인 프로필, 작업 메모, 코드 지식)를 공유 |
| 지인/오픈소스 사용자 (2차) | ~10 | 셀프호스팅 또는 무료 가입으로 동일 기능 활용. 본인 데이터 export·계정 삭제 보장 |
| 자동화 스크립트 (3차) | N/A | API 키로 REST 호출, 외부 봇/CLI에서 메모리 read/write |

## 핵심 문제

1. 기존 AI는 매 세션이 무상태. ChatGPT에 저장한 정보를 Claude는 모른다.
2. AI별 메모리 기능이 있어도 도구 간 공유 불가.
3. AI 메모리에 저장된 데이터를 사용자가 export·이동하기 어렵거나 불가능.
4. 매번 같은 설명을 반복하는 비용 (시간·토큰·집중력).

## 핵심 기능

1. **MCP 서버 + REST API 이중 채널** — MCP 지원 클라이언트는 OAuth로, 미지원 클라이언트는 API 키로 동일 데이터에 접근
2. **자동 분류·요약·임베딩** — 자연어 입력을 받아 폴더 경로 추론, 제목·요약 생성, 태그 자동 부여, 벡터 임베딩으로 저장
3. **하이브리드 검색** — 의미(임베딩) + 키워드(전문검색) 결합, 사용자 조정 가능한 가중치
4. **대시보드** — Google Drive 스타일 폴더 브라우저, 마크다운 미리보기·인라인 편집, 검색·필터·정렬, 키 관리, 연결 가이드, 사용량 모니터링
5. **본문 `[[wiki-link]]` + 관계 맵** — 메모리 본문 안 `[[메모리 제목]]` / `[[mem_<id>]]`를 파싱해 자동으로 양방향 backlink 인덱스 유지. `/map` 페이지에서 그래프 시각화. LLM이 `mn_write`/`mn_update` 시 관련 기존 메모리를 자연스럽게 `[[link]]`로 참조하도록 시스템 프롬프트 유도 (옵시디언적 경험). `index.md` 자동 유지·lint 도구는 후속
6. **데이터 포터빌리티** — export(zip + manifest.json), import(같은 형식 또는 일반 마크다운 zip), 계정 삭제 시 완전 제거
7. **사용자 가시성** — 일일 토큰 사용량, 임계치 경고, 본인 감사 로그 조회, 키별 마지막 사용 시각

## 도메인 모델

### Memory (메모리)
- **단위**: 하나의 마크다운 문서 = 하나의 토픽
- **본문**: 마크다운(GitHub-flavored), 선택적 YAML frontmatter
- **메타**: id, title, summary, folder, tags[], created_at, updated_at, source_uri, model_version
- **본문 크기 상한**: 256KB (이미지 등 바이너리는 별도 첨부 모델, MVP 제외)
- **소속**: 폴더는 정확히 1개, 태그는 0개 이상
- **수정**: 낙관적 락(`updated_at` 비교), 충돌 시 사용자에게 두 버전 제시

### Folder (폴더)
- **계층**: 최대 깊이 8 (UI 가독성·쿼리 효율 한계)
- **경로 표기**: `/projects/mneme/decisions/` (Unix 스타일)
- **이름 제약**: 영숫자·하이픈·언더스코어·공백·한글, 슬래시 금지, 100자 이내
- **이동**: 폴더 이동 시 자식 메모리의 정규화 경로(materialized path) 단일 트랜잭션 일괄 갱신
- **빈 폴더**: 자동 삭제하지 않음(사용자가 의도적으로 비울 수 있음)

### Tag (태그)
- **다중**: 한 메모리에 0~16개
- **자유 입력**: 사전 정의 없음, LLM이 분류 시 자동 부여, 사용자 수정 가능
- **표기**: 영숫자·하이픈·한글, 32자 이내, 대소문자 무시(저장은 소문자)

### Memory ID
- **형식**: UUID v7 (시간순 정렬 + 충돌 회피)
- **외부 노출**: `mem_<base32 26자>` — URL/CLI 친화. 예: `mem_2v8gqj4z7n3kh5p9r1t6w0xy2a`
- **내부 저장**: UUID v7 그대로 (인덱스 효율)

## MVP 범위 (구현 완료, 2026-06-28)

> 모든 항목 ✅ = phase 01~17 + 21~23 + 31~32 완료. ⚠ = 부분 / 사용자 검증 대기. ❌ = 의도 deferred.

- ✅ Google OAuth 로그인 + `mn_<32B base62>` API 키 발급/폐기/회전 (phase 03·04)
- ✅ MCP 서버(SSE + 메시지 엔드포인트) + 11개 도구. 본문 `[[link]]` 자동 인덱스 + `mn_relations` (phase 09·16)
- ✅ MCP OAuth DCR + Authorization Code(PKCE) + access/refresh 토큰 (phase 10)
- ✅ REST API — MCP와 동일 기능, 대시보드도 같은 API 사용 (phase 05~07)
- ✅ 메모리 저장 시 OpenAI 임베딩 + gpt-4o-mini 자동 분류·요약·태깅 (phase 06)
- ✅ PostgreSQL + pgvector 하이브리드 검색 (벡터 α + tsvector β + 트라이그램 γ, env로 조정) (phase 07)
- ✅ 대시보드: 폴더 트리·마크다운 뷰어/편집(낙관적 락 ConflictPanel)·검색·아카이브·키 빌더 (phase 11~12)
- ✅ `/map` force-directed 그래프 + 깨진 링크 사이드 패널 + 메모리 상세 backlink 패널 (phase 17)
- ✅ `/archive` 페이지 + 복구 (활성 동일 제목 충돌 시 409) (phase 11 step 3)
- ⚠ 키보드 단축키: `⌘/Ctrl+K`(검색 포커스)만 구현. 나머지(`Cmd+N`/`Cmd+S` 등)는 후속.
- ✅ API 키 관리 UI + MCP 연결 명령 빌더(Claude Desktop·Claude Code·Codex CLI 스니펫)
- ✅ 사용자별 데이터 격리: 모든 리포지토리 user_id 강제 + `IsolationRegressionTest`(서비스·MCP 도구) (phase 02~08·09)
- ✅ Rate limit(Caffeine 분/일/쓰기 분리) + 토큰 한도 가드(`TokenQuotaGuard`) (phase 08)
- ⚠ 사용량 추적 + 대시보드 배너 ✅ (`/usage`). 이메일 임계치 알림은 phase 33 deferred
- ✅ Export `GET /api/export` zip(manifest.json + memories/*.md frontmatter) (phase 13)
- ✅ Import 2단계 흐름(preview → apply, 항목별 skip/replace/create-new) (phase 13)
- ✅ 감사 로그 — 본인 이벤트만 `/audit` (응답에서 ip/UA 노출 금지) (phase 14)
- ✅ 온보딩 4단계 투어 + `/connect` 클라이언트 가이드 페이지 (phase 12)
- ✅ Docker Compose 단일 명령 실행 (phase 01)
- ✅ 셀프호스팅 가이드(`docs/SELFHOST.md` + `docs/TROUBLESHOOTING.md` + `docs/BACKUP.md`) (phase 31~32)

### M8 Wiki 확장 (추가 구현, 2026-06-28)

- ✅ 폴더별 LLM 합성 인덱스 — 주제별 그루핑 + `[[wiki-link]]` 자동 + 빈 곳 추측 (phase 21)
- ✅ 검토(lint) — broken / orphan / stub / dup-title 4룰 (phase 22)
- ✅ 피드백 학습 — 👍/👎 + 노트 → 다음 LLM 호출 system prompt 자동 inject (phase 23)
- ✅ 사용 가이드 페이지 `/help` — 서비스·페이지·MCP 도구·내부 흐름·FAQ 한곳에 정리

## MVP 제외 사항 (당분간)

- 사용자별 토큰 사용량 임계치 이메일/Slack 알림(phase 33)
- 메모리 본문 envelope encryption — 디스크 암호화로 갈음(phase 34)
- 첨부 파일·이미지(phase 35)
- 메모리/폴더 읽기 전용 공유 링크(phase 36)
- 추가 OAuth 제공자 GitHub/Apple 등(phase 37)
- 모바일 PWA(phase 38)
- 외부 호스팅·도메인·HTTPS — 사용자 결정 대기(phase 30)
- LLM 기반 모순 감지(현재 lint는 규칙 기반 4룰만)
- 백그라운드 자동 관계 추론. 관계는 본문 `[[link]]`로만, 명령형으로만 생성
- 검토(review) 패널, 사용자 피드백 학습 루프
- 폴더·메모리 공유, 다중 사용자 협업, 읽기 전용 공개 링크
- 결제·구독·요금제
- Gemini 웹 직접 연결 (Google이 MCP를 열기 전까지 불가, REST 우회만 가능)
- 모바일 전용 앱 (대시보드는 데스크톱 우선, 모바일에선 검색·읽기만 부분 동작)
- 메모리 본문 at-rest 컬럼 암호화
- 첨부 파일/이미지 업로드 (외부 이미지 URL은 허용)
- GitHub 등 추가 OAuth 제공자
- 영문 UI 문구 (i18n 구조만, 문구는 한국어)
- 알림 채널 다양화 (MVP는 이메일만)
- 자체 모델 호스팅 / 로컬 LLM 옵션

## 성공 기준

| 항목 | 목표 |
|---|---|
| 다중 AI 공유 | Claude.ai에서 저장한 메모리를 ChatGPT 개발자 모드에서 즉시 검색 가능 |
| 저장 응답 시간 | P95 < 8초 (임베딩 + LLM 분류 + INSERT) |
| 검색 응답 시간 | P95 < 1초 (10K 메모리 기준) |
| 동시 사용자 | 단일 VPS(2 vCPU / 4GB)에서 10명 동시 안정 |
| 셀프호스팅 | `docker compose up` 한 번으로 동일 환경 구성 |
| 데이터 격리 | 통합 테스트가 모든 API/MCP 경로에서 교차 접근 → 404 검증 |
| 포터빌리티 | export 결과를 새 인스턴스에 import해 동일 상태 복원 |
| 비용 가드 | 키 1개 도용 시 일 한도 초과 30초 이내 차단 |
| 계정 삭제 | 요청 후 30일 유예, 그 사이 복구 가능, 이후 데이터 완전 제거 |

## 비기능 요구사항

- **시간대**: 서버는 UTC 저장, 대시보드는 사용자 로컬 시간 표시(`Intl.DateTimeFormat`). 사용자 locale은 `users.locale`
- **언어**: 백엔드 에러 코드는 영어 식별자(`ERR_RATE_LIMIT` 등), 사용자 노출 문구는 i18n 키 → 한국어 번역
- **동시 편집**: 낙관적 락. 충돌 시 두 버전 diff 표시 후 사용자 병합
- **임베딩 재계산**: 본문 수정 시 비동기 잡으로 재임베딩. 그 사이는 옛 임베딩으로 검색
- **검색 정확도**: 한국어는 PostgreSQL `pg_trgm` 트라이그램 매칭 + 임베딩 결합. 후속에 `mecab-ko` 검토
- **가용성**: 95% (개인 사용 기준, SLA 아님). 백업으로 데이터 손실 0
- **데이터 보존**: archived 메모리는 영구 보존(soft delete). 사용자 계정 삭제 시에만 30일 후 완전 제거

## 리스크와 제약

| 리스크 | 완화 |
|---|---|
| OpenAI 비용 폭주 | 사용자별 일/월 토큰 상한 + 임계치 알림 + 키 도용 패턴 감지(IP/UA 이상치) |
| MCP 스펙 변화 | Spring AI MCP starter 의존, 통합 테스트로 회귀 방지, ADR로 변경 추적 |
| Gemini 웹 미지원 | 제어 불가, REST API 우회. 공식 지원 시 자동 커버 |
| 임베딩 모델 교체 | `model_version` 컬럼 + 백필 잡, 신·구 임베딩 병행 기간 운영 |
| 데이터 격리 회귀 | 모든 리포지토리 메서드 user_id 강제 + 격리 회귀 테스트 클래스 |
| 프롬프트 인젝션 | 시스템 프롬프트 상수화, 사용자 입력은 인용 블록, 응답에서 우회 패턴 감지 |
| 민감정보 유출 | 로그·에러 응답에서 본문/키/토큰 자동 마스킹 |
| OAuth 리다이렉트 URI 부정 | 화이트리스트 엄격 매칭, 와일드카드 금지 |
| 셀프호스팅 진입장벽 | `.env.example` + 스크린샷 + Docker 단일 명령 |
| 작성자 1인 유지보수 | ADR로 의사결정 외부화, CRITICAL 규칙, 자동 테스트가 안전망 |
| Postgres 단일 장애 | 일 1회 pg_dump → 외부 객체 스토리지, restore 스크립트 |
