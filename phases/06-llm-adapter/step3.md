# step 3 — prompt-guard-chat

## 목적

`gpt-4o-mini` 요약/분류/태그 제안 + 프롬프트 인젝션 방어.

## 범위

- `PromptGuard.quote(userText)`: 사용자 본문을 `<<<USER_CONTENT>>>` 펜스 안으로 캡슐화 + 길이 8KB 절단.
- `ChatService`: `summarize(text)`, `classifyFolder(text, candidatePaths)`, `suggestTags(text, max=5)`.
- 시스템 프롬프트는 별도 리소스(`/llm/prompts/*.md`)로 분리.
- 호출 실패는 `OpenAiException` → 메모리 저장은 막지 않고 요약/태그만 비어서 진행.

## Acceptance

- `./gradlew :backend:test --tests "*ChatService*"` 통과(mock 응답).
