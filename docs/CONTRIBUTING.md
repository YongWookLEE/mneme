# Contributing

Mneme는 비상업·1인 유지보수 프로젝트지만, 셀프호스팅 사용자와 외부 기여자가 편하게 참여할 수 있도록 다음 가이드를 따른다.

## 두 종류의 사용자

### 셀프호스팅 사용자

직접 운영하는 인스턴스를 띄우는 사용자.

- 우선 [`README.md`](../README.md) 빠른 시작 → [`docs/DEVELOPMENT.md`](DEVELOPMENT.md) 환경 설정
- 운영 배포 단계별 가이드: [`docs/SELFHOST.md`](SELFHOST.md)
- 자주 막히는 곳: [`docs/TROUBLESHOOTING.md`](TROUBLESHOOTING.md)
- 백업·복구: [`docs/BACKUP.md`](BACKUP.md)
- 보안 체크리스트: [`docs/SECURITY.md`](SECURITY.md)
- 문제 발생 시: GitHub Discussions (예정) 또는 이슈

### 코드 기여자

PR을 제출하려는 사용자.

다음 절차를 따라주세요.

## 기여 절차

### 1. 이슈 먼저 열기

큰 변경(새 기능, 아키텍처 영향, 외부 의존성 추가)은 이슈로 제안하고 합의 후 PR. 작은 버그 수정·문서 오타는 바로 PR.

### 2. 브랜치 전략

- main 브랜치 보호
- 작업 브랜치: `feat/<짧은-설명>`, `fix/<짧은-설명>`, `docs/<...>`, `refactor/<...>`
- PR은 main으로

### 3. 개발 환경

[`docs/DEVELOPMENT.md`](DEVELOPMENT.md) 그대로 따라 설정.

### 4. 코딩 규칙

#### 백엔드 (Kotlin)
- JDK 21, Spring Boot 3.x
- 패키지: `com.mneme.<module>`
- 모듈 경계: [`docs/ARCHITECTURE.md` 모듈 경계](ARCHITECTURE.md#모듈-경계)
- 함수/메서드/클래스에 한국어 KDoc 주석 (`/** 설명 */`)
- 린트: `./gradlew :backend:ktlintCheck`
- 테스트 우선 (TDD): 새 기능은 실패하는 테스트부터

#### 프론트엔드 (TypeScript/React)
- 함수 컴포넌트만, 클래스 컴포넌트 금지
- TanStack Query로 서버 상태, useState로 로컬 UI 상태
- 컴포넌트 1개 = 1 파일, 100줄 넘으면 분할 검토
- 단축키는 `lib/shortcuts.ts`에 등록
- 린트: `npm --prefix frontend run lint`

#### 공통
- 의미 있는 식별자, 줄임말 지양
- 주석은 "왜"를 설명, "무엇"은 코드가 말함
- 사용자 입력 검증은 서버에서 진실(`jakarta.validation`)

### 5. CRITICAL 규칙 준수

[`CLAUDE.md`](../CLAUDE.md)와 [`AGENTS.md`](../AGENTS.md)의 CRITICAL 규칙은 PR 리뷰의 첫 체크리스트. 위반 시 리뷰 거절.

특히:
- 모든 리포지토리 메서드는 user_id 첫 인자
- 새 엔드포인트는 `IsolationRegressionTest`에 케이스 추가
- `@Transactional`은 서비스만, 컨트롤러 금지
- 외부 호출(LLM/이메일)은 트랜잭션 밖에서
- 사용자 입력 SQL/프롬프트 직접 보간 금지
- Flyway forward-only

### 6. 테스트

- 단위 테스트는 모든 도메인 로직에 필수
- 통합 테스트는 Testcontainers 기반
- 격리 회귀 테스트 케이스 추가는 신규 엔드포인트의 acceptance criteria
- 커밋 전 로컬 `./gradlew :backend:test` 통과 확인

### 7. 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/) 형식:

```
<type>(<scope>): <subject>

<body>

<footer>
```

타입:
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서만
- `refactor`: 동작 변경 없는 코드 정리
- `test`: 테스트 추가/수정
- `chore`: 빌드/설정
- `security`: 보안 수정
- `perf`: 성능 개선

예:
```
feat(memory): add tag editing in inline editor

낙관적 락 충돌 시 두 버전 diff 표시.
태그는 자동완성 다중 선택 컴포넌트 재사용.
```

### 8. PR 체크리스트

PR 본문에 다음을 채워주세요:

- [ ] 관련 이슈 링크
- [ ] 변경 요약 (한 문단)
- [ ] CRITICAL 규칙 위반 여부 자가 점검 결과
- [ ] 새 엔드포인트가 있다면 `IsolationRegressionTest` 추가 여부
- [ ] 새 ADR이 필요한 결정이 있다면 docs/ADR.md 갱신 여부
- [ ] `CHANGELOG.md` 갱신 (사용자 가시 변경 시)
- [ ] `docs/HANDOFF.md` 갱신 (phase 변경 시)
- [ ] `./gradlew :backend:test` 통과
- [ ] `npm --prefix frontend run test` 통과
- [ ] 스크린샷 (UI 변경 시)

### 9. 리뷰

- 리뷰는 1인 유지보수자(현 작성자) 1명
- 첫 응답은 7일 이내 목표
- 작은 PR은 빨리, 큰 PR은 분할 요청 가능

### 10. 머지

- Squash merge가 기본
- 머지 후 작업 브랜치 삭제
- 마일스톤 도달 시 `CHANGELOG.md` 갱신

## 문서 기여

문서만 수정하는 경우도 환영. 다만 다음 파일들은 동기화 유지가 중요:

- `CLAUDE.md` ↔ `AGENTS.md` (CRITICAL 규칙, 명령어는 두 파일 동일)
- `docs/PRD.md` ↔ `docs/ROADMAP.md` (MVP 범위와 phase가 일치)
- `docs/ARCHITECTURE.md` ↔ `docs/ADR.md` (결정이 아키텍처에 반영됨)

## 기여자 행동 강령

- 존중과 호의: 코드 리뷰는 기술 대상으로, 사람 대상 아님
- 사용자 데이터 안전이 모든 결정의 우선순위
- 의심스러운 일은 머지 전에 묻기

## 라이선스

PR은 프로젝트와 같은 [MIT 라이선스](../LICENSE)로 기여됩니다.
