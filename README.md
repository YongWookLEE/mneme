# Mneme

> 그리스 기억의 뮤즈. 여러 AI 클라이언트(Claude, ChatGPT, Codex, Grok 등)가 공유하는 단일 영구 기억층.

## 무엇인가

대부분의 AI는 매 세션이 무상태다. ChatGPT에 저장한 내용을 Claude는 모르고, Codex는 또 다른 컨텍스트를 가진다. **Mneme**는 그 사이에 놓이는 사용자 소유의 메모리 저장소다. MCP(Model Context Protocol) 서버 + REST API로 모든 AI 도구에 같은 기억을 공급한다.

- **저장**: 자연어로 "이거 기억해줘" → 자동 분류·요약·임베딩·태깅
- **검색**: 의미(임베딩) + 키워드(전문검색) 하이브리드
- **공유**: Claude.ai · ChatGPT 개발자 모드 · Codex CLI · REST 클라이언트 동일 데이터
- **소유**: 셀프호스팅 가능, 데이터 export·import, 계정 삭제 시 완전 제거

비상업, 셀프호스팅 또는 작성자 운영 인스턴스 무료 사용 (~10명 규모 목표).

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

## 문서

| 파일 | 내용 |
|---|---|
| [`docs/PRD.md`](docs/PRD.md) | 무엇을·왜 — 목표, 사용자, 도메인 모델, MVP 범위 |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 어떻게 — 디렉토리, 모듈, 데이터 모델, 보안, 관측성, 백업 |
| [`docs/ADR.md`](docs/ADR.md) | 왜 그 결정 — 17개 의사결정과 트레이드오프 |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | 어디까지 — 마일스톤 M1~M9, phase 표 |
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | 지금 어디 — 다음 세션 진입점 |
| [`docs/UI_GUIDE.md`](docs/UI_GUIDE.md) | 화면 — 톤, 색, 페이지, 단축키 |
| [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) | 로컬 개발 — env 변수, OAuth/OpenAI 발급, 자주 쓰는 명령 |
| [`docs/SECURITY.md`](docs/SECURITY.md) | 보안 — 정책, 취약점 신고 |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | 기여·셀프호스팅 가이드 |
| [`CHANGELOG.md`](CHANGELOG.md) | 변경 이력 |

## 라이선스

[MIT](LICENSE).
