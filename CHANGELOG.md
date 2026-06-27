# Changelog

[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 형식, [Semantic Versioning](https://semver.org/spec/v2.0.0.html) 지향.

## [Unreleased]

### Added
- 프로젝트 부트스트랩: PRD, ARCHITECTURE, ADR(001~019), ROADMAP, UI_GUIDE, HANDOFF, DEVELOPMENT, SECURITY, CONTRIBUTING 문서 작성
- 도메인 모델 명세 (Memory/Folder/Tag/MemoryLink/Memory ID)
- 환경 변수 전체 명세 (`deploy/.env.example`)
- 보안 정책 (격리, rate limit, 토큰 한도, 헤더, PII 마스킹)
- 데이터 포터빌리티 정책 (export + import)
- MIT 라이선스
- MVP 정체성 확장: 본문 `[[wiki-link]]` 파싱 + `/map` 그래프 + `/archive` + `/keys` MCP 명령 빌더 (ADR-018, ADR-019)
- phase 16 wiki-link-parser, phase 17 memory-map-ui
- Phase 01 project-skeleton:
  - Gradle 8.7 + Kotlin 1.9.25 + Spring Boot 3.3.4 백엔드 모듈
  - Vite 5 + React 18 + TypeScript 5 + Tailwind 3 프론트엔드 모듈
  - Docker Compose 스택: postgres(pgvector pg16) + backend + frontend-dev + caddy
  - GitHub Actions CI: ktlint, JUnit + Kotest, ESLint, Vitest, build 검증
  - 백엔드 스모크 테스트, 프론트엔드 App 렌더 테스트
- 디자인 정책: 중립 모노크롬 다크 테마 확정(`docs/design/` 참고 이미지 2장 추가)

### Changed
- 프로젝트명을 `unified-memory` → `Mneme`로 변경
- Kotlin 패키지 루트 `com.mneme`
- ADR-003 개정: Heirmos 골격 + LLM Wiki 본문 [[link]]를 MVP 핵심 정체성으로 채택
- phase 20 wiki-links를 deferred → MVP phase 16으로 승격

### 향후 마일스톤 (미릴리즈)

#### M1 — 로컬 부팅
- phase 01 ✅ / phase 02 (persistence-base) 진행 시 0.1.0 후보

#### M5 — MCP 라이브
- phase 09-10 완료 시 0.5.0 후보

#### M7 — 포터빌리티·운영
- phase 13-15 완료 시 1.0.0-rc 후보
