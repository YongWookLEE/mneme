# P05 Step 2: memory-domain-rest

> Memory CRUD — 코어 필드(title/content/folderId)만. embedding/tsv/summary는 phase 06. archive/restore도 함께.
> 본문 256KB 상한 검사. archived는 별도 endpoint.

## 산출물

- `MemoryService`: create / get / update(낙관적 락) / archive / restore / list / listArchived
- `MemoryController`: POST/GET/PATCH/POST archive|restore/DELETE
- DTO: `MemoryRequest/Response`
- 409 충돌(낙관적 락): ObjectOptimisticLockingFailureException → ApiError.CONFLICT
