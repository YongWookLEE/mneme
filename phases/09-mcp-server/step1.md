# Step 1 — Spring AI MCP Server starter 도입

## 범위

- `backend/build.gradle.kts`에 Spring AI BOM(`spring-ai-bom:1.0.0`) + `spring-ai-starter-mcp-server-webmvc` 의존성 추가
- `application.yml`에 `spring.ai.mcp.server.*` 기본 설정(name, version, type=SYNC, transport, request-timeout)
- `com/mneme/mcp/` 패키지 신설

## Acceptance

- `./gradlew :backend:dependencies` 출력에 `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` 포함
- 백엔드 부팅 후 `GET /actuator/health` 통과 (MCP starter가 다른 빈을 깨뜨리지 않음)
