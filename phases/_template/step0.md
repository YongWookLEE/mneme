# Step 0: example-step

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/HANDOFF.md`
- `docs/ROADMAP.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`

## 작업

{THIS_STEP_WORK_SCOPE}

이 step에서 수정할 파일:

- `{FILE_PATH_1}`: {CHANGE_SUMMARY_1}
- `{FILE_PATH_2}`: {CHANGE_SUMMARY_2}

## Acceptance Criteria

```bash
{LINT_COMMAND}
{TEST_COMMAND}
```

## 검증 절차

1. 위 Acceptance Criteria 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - `docs/ARCHITECTURE.md`의 디렉터리 구조를 따르는가?
   - `docs/ADR.md`의 기술 선택을 벗어나지 않았는가?
   - `AGENTS.md` 또는 `CLAUDE.md`의 CRITICAL 규칙을 위반하지 않았는가?
3. 결과에 따라 `phases/{phase-name}/index.json`의 해당 step을 업데이트한다:
   - 성공: `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패: `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요: `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단
4. `docs/HANDOFF.md`를 다음 세션이 이어갈 수 있게 갱신한다.

## 금지사항

- {FORBIDDEN_ACTION}. 이유: {REASON}
- 이 step 범위 밖의 기능을 추가하지 마라.
- 기존 테스트를 깨뜨리지 마라.
