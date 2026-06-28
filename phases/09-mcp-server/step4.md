# Step 4 — 도구 단위 격리 테스트

## 범위

- `McpToolsIsolationTest`: 사용자 A의 ID로 사용자 B 컨텍스트에서 read/update/archive/restore/relations → 404 검증
- mock 기반(서비스 레이어). REST/HTTP/Streamable은 phase 15(client-validation)에서 라이브 통합.

## Acceptance

- `./gradlew :backend:test --tests "*McpToolsIsolationTest"` 통과
