# Step 3 — 11개 mn_* 도구 구현

## 도구 목록 (PRD §11)

1. `mn_schema` — 도구 카탈로그 + 서버 메타데이터 반환 (도큐먼트용)
2. `mn_whoami` — 현재 사용자 정보 (extId, email)
3. `mn_list` — 폴더/메모리 트리 또는 평탄 목록 + 필터(folder, archived)
4. `mn_read` — 단건 메모리(본문 포함) 조회
5. `mn_search` — 하이브리드 검색 (위임: SearchService)
6. `mn_write` — 메모리 생성 (위임: MemoryWriteFacade.create)
7. `mn_update` — 본문/제목/요약/폴더 갱신 (위임: MemoryWriteFacade.update, 낙관적 락 version 필수)
8. `mn_archive` — 메모리 archive
9. `mn_restore` — 메모리 restore
10. `mn_relations` — 메모리의 forward/backward link + broken link (memory_links 인덱스 조회. phase 16 본문 파서가 채움. 본 phase에서는 조회만)
11. `mn_surface` — 컨텍스트 기반 후보 메모리 (검색 wrapper, query=hint)

## 규칙

- 모든 도구는 `userResolver.currentUserId()`로 시작
- 외부 ID 입출력은 base32 prefix (`mem_`/`fld_`/`tag_`)
- 도구 반환은 `Map<String, Any?>` 또는 data class — Jackson 직렬화 우호적
- 본문은 `mn_read`/`mn_write`/`mn_update` 외에는 포함하지 않음 (목록은 summary만)

## Acceptance

- `./gradlew :backend:build` 통과
- `MnemeTools`가 11개 메서드 모두 `@Tool(name = ...)` 등록
- `McpToolsConfig`가 `MethodToolCallbackProvider` 빈 등록
