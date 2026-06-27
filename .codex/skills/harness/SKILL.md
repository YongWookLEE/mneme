---
name: harness
description: Read project docs, design scoped phase steps, create phases files, and run the Harness workflow with Codex.
---

이 프로젝트는 Basic Harness 템플릿을 사용한다. Codex는 `AGENTS.md`, `docs/`, `phases/`를 기준으로 아래 워크플로우를 따른다. Claude 설정은 `CLAUDE.md`와 `.claude/`에 별도로 유지되며, Codex 실행은 `scripts/codex_execute.py`를 사용한다.

## 워크플로우

### A. 탐색

`docs/HANDOFF.md`를 가장 먼저 읽고 현재 상태를 파악한다. 이어서 `docs/ROADMAP.md`, `docs/PRD.md`, `docs/ARCHITECTURE.md`, `docs/ADR.md`, 필요한 경우 `docs/UI_GUIDE.md`를 읽고 프로젝트의 기획, 아키텍처, 설계 의도를 파악한다.

### B. 논의

구현을 위해 구체화하거나 기술적으로 결정해야 할 사항이 있으면 사용자에게 제시하고 논의한다.

### C. Step 설계

사용자가 구현 계획 작성을 지시하면 여러 step으로 나뉜 초안을 작성해 피드백을 요청한다.

설계 원칙:

1. **Scope 최소화** - 하나의 step에서 하나의 레이어 또는 모듈만 다룬다. 여러 모듈을 동시에 수정해야 하면 step을 쪼갠다.
2. **자기완결성** - 각 step 파일은 독립된 Codex 세션에서 실행된다. "이전 대화에서 논의한 바와 같이" 같은 외부 참조는 금지한다. 필요한 정보는 전부 파일 안에 적는다.
3. **사전 준비 강제** - 관련 문서 경로와 이전 step에서 생성/수정된 파일 경로를 명시한다.
4. **시그니처 수준 지시** - 함수/클래스의 인터페이스만 제시하고 내부 구현은 에이전트 재량에 맡긴다. 핵심 규칙은 반드시 명시한다.
5. **AC는 실행 가능한 커맨드** - `npm run build && npm test` 같은 실제 검증 커맨드를 포함한다.
6. **주의사항은 구체적으로** - "X를 하지 마라. 이유: Y" 형식으로 적는다.
7. **네이밍** - step name은 kebab-case slug로 표현한다.

### D. 파일 생성

사용자가 승인하면 아래 파일들을 생성한다. 새 phase 작성 시 `phases/_template/`의 형식을 참고한다.

#### `phases/index.json`

```json
{
  "phases": [
    {
      "dir": "0-mvp",
      "status": "pending"
    }
  ]
}
```

#### `phases/{task-name}/index.json`

```json
{
  "project": "<프로젝트명>",
  "phase": "<task-name>",
  "steps": [
    { "step": 0, "name": "project-setup", "status": "pending" },
    { "step": 1, "name": "core-types", "status": "pending" },
    { "step": 2, "name": "api-layer", "status": "pending" }
  ]
}
```

#### `phases/{task-name}/step{N}.md`

각 step 파일에는 읽어야 할 파일, 작업 범위, Acceptance Criteria, 검증 절차, 금지사항을 포함한다.

상태 필드 규칙:

- `project`: 프로젝트명. `AGENTS.md`를 우선 참조한다.
- `phase`: task 이름. 디렉토리명과 일치시킨다.
- `steps[].step`: 0부터 시작하는 순번.
- `steps[].name`: kebab-case slug.
- `steps[].status`: 초기값은 모두 `"pending"`.
- `summary`: step 완료 시 다음 step에 유용한 산출물 한 줄 요약.
- `created_at`, `started_at`, `completed_at`, `failed_at`, `blocked_at`은 실행기가 자동 기록한다. 생성 시 넣지 않는다.

#### `docs/ROADMAP.md`와 `docs/HANDOFF.md`

새 phase를 생성하거나 완료 상태를 바꾸면 아래 문서도 함께 갱신한다.

- `docs/ROADMAP.md`: 전체 phase 표와 상태.
- `docs/HANDOFF.md`: 다음 세션이 즉시 이어갈 현재 phase/step, 최근 검증 결과, 주의사항.

### E. 실행

```bash
python3 scripts/codex_execute.py {task-name}
python3 scripts/codex_execute.py {task-name} --push
```

실행기는 `feat-{task-name}` 브랜치 생성/checkout, 가드레일 주입, 완료된 step summary 누적, 최대 3회 재시도, 커밋, 타임스탬프 기록을 자동 처리한다.

에러 복구:

- error 발생 시: `phases/{task-name}/index.json`에서 해당 step의 `status`를 `"pending"`으로 바꾸고 `error_message`를 삭제한 뒤 재실행한다.
- blocked 발생 시: `blocked_reason`의 사유를 해결한 뒤 `status`를 `"pending"`으로 바꾸고 `blocked_reason`을 삭제한 뒤 재실행한다.
