# P05 Step 3: tag + memory-tag

> Tag CRUD + 메모리에 태그 부착/해제. `/api/memories/{extId}/tags` 엔드포인트.

## 산출물

- `TagService`: getOrCreate(이름 정규화), listForMemory, listAll
- `TagController`: GET /api/tags 사용자 전체 태그
- `MemoryController` 확장: GET / POST / DELETE /api/memories/{extId}/tags
- 정규화: 소문자, 32자 상한, 한 메모리 최대 16개
