# Mneme

> 그리스 기억의 뮤즈. 여러 AI 클라이언트(Claude, ChatGPT, Codex, Grok 등)가 공유하는 단일 영구 기억층.

[![status](https://img.shields.io/badge/status-MVP+Wiki%20Extension-success)](docs/ROADMAP.md)
[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

## 무엇인가

대부분의 AI는 매 세션이 무상태다. ChatGPT에 저장한 내용을 Claude는 모르고, Codex는 또 다른 컨텍스트를 가진다. **Mneme**는 그 사이에 놓이는 사용자 소유의 메모리 저장소다. MCP(Model Context Protocol) 서버 + REST API로 모든 AI 도구에 같은 기억을 공급한다.

- **저장**: 자연어로 "이거 기억해줘" → 자동 분류·요약·임베딩·태깅
- **검색**: 의미(임베딩) + 키워드(전문검색) + 트라이그램 하이브리드(가중치 조정 가능)
- **연결**: 본문 안 `[[wiki-link]]`로 메모리끼리 묶고 `/map`에서 그래프로 시각화. 제목 변경 시 backlink 본문도 자동 일괄 치환
- **공유**: Claude Desktop · Claude Code · Codex CLI · ChatGPT Developer mode · REST 클라이언트 동일 데이터
- **자기 개선**: 사용자가 👍/👎 피드백을 남기면 다음 LLM 호출 system prompt에 자동 반영
- **검토**: `/lint`에서 깨진 링크·외톨이·미완성·중복 자동 감지
- **소유**: 셀프호스팅 가능, 데이터 export·import, 계정 삭제 시 완전 제거

비상업, 셀프호스팅 또는 작성자 운영 인스턴스 무료 사용 (~10명 규모 목표).

## 현재 상태 (2026-06-28)

- MVP 마일스톤 **M1 ~ M8 전부 달성**: 로컬 부팅 / 인증 / 메모리 도메인 / 보안 완전 / MCP 라이브 / 대시보드 / 위키 경험 / 포터빌리티 + 관측성 / Wiki 확장
- **17개 phase + 3개 deferred 운영 안착 문서**(SELFHOST · BACKUP · TROUBLESHOOTING) 완료
- 클라이언트 라이브 검증: **Codex CLI ✅** (Claude Desktop · ChatGPT 사용자 검증 대기)
- 운영 호스팅(phase 30)은 사용자 결정 대기로 deferred. 현재는 로컬 단독 사용.

## 빠른 시작 (로컬 개발)

전제: Docker 26+, Git, OpenAI API 키, Google OAuth 클라이언트.

```bash
git clone <repo-url> mneme
cd mneme
cp deploy/.env.example deploy/.env
# deploy/.env 파일 열어서 MNEME_OPENAI_API_KEY, MNEME_GOOGLE_OAUTH_* 채우기
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

확인:
- 백엔드 헬스: <http://localhost:8080/actuator/health>
- 대시보드: <http://localhost:5173>
- DB: `docker compose exec postgres psql -U mneme -d mneme`

상세한 환경 변수와 OAuth 등록 단계는 [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).

## 기술 스택

- **백엔드**: Kotlin + Spring Boot 3.x (JDK 21)
- **DB**: PostgreSQL 16 + pgvector + pg_trgm
- **LLM**: OpenAI (`text-embedding-3-small`, `gpt-4o-mini`) via Spring AI
- **MCP**: Spring AI MCP Server (Streamable HTTP)
- **프론트엔드**: React + Vite + TypeScript + shadcn/ui + TanStack Query + react-i18next
- **인증**: Google OAuth + `mn_` API 키(sha256) + OAuth DCR
- **인프라**: Docker Compose + Caddy(자동 HTTPS)

## 사용 가이드 (대시보드 안)

대시보드 우상단 **도움말** 탭(<http://localhost:5173/help>)에 무엇이고 어떻게 동작하는지 한곳에 정리되어 있다. 처음 들어가면 4단계 온보딩 투어가 자동으로 뜬다.

## 문서 (저장소)

| 파일 | 내용 |
|---|---|
| [`docs/PRD.md`](docs/PRD.md) | 무엇을·왜 — 목표, 사용자, 도메인 모델, MVP 범위 |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 어떻게 — 디렉토리, 모듈, 데이터 모델, 보안, 관측성, 백업 |
| [`docs/ADR.md`](docs/ADR.md) | 왜 그 결정 — 의사결정 25개와 트레이드오프 |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | 어디까지 — 마일스톤 M1~M9, phase 표 |
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | 지금 어디 — 다음 세션 진입점 |
| [`docs/UI_GUIDE.md`](docs/UI_GUIDE.md) | 화면 — 톤, 색, 페이지, 단축키 |
| [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) | 로컬 개발 — env 변수, OAuth/OpenAI 발급, 자주 쓰는 명령 |
| [`docs/SECURITY.md`](docs/SECURITY.md) | 보안 — 정책, 취약점 신고 |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | 기여 가이드 |
| [`docs/SELFHOST.md`](docs/SELFHOST.md) | 셀프호스팅 단계별 안내 (서버·OAuth·OpenAI·첫 키 발급) |
| [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) | 자주 막히는 문제와 해결 |
| [`docs/BACKUP.md`](docs/BACKUP.md) | 백업·복구(pg_dump cron + 객체 스토리지) |
| [`CHANGELOG.md`](CHANGELOG.md) | 변경 이력 |

## 라이선스

[MIT](LICENSE).
