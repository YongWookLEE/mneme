# P05 Step 1: folder-domain-rest

> FolderService: 폴더 CRUD + materialized path 자동 계산 + 이동 시 자식 path 일괄 갱신. FolderController: REST.

## 산출물

- `com.mneme.memory.FolderService`: create, rename, move, listByUser, listChildren
- `com.mneme.api.FolderController`: POST/GET/PATCH/DELETE /api/folders
- Path 규칙: 루트 `/`, 정규화 `/projects/mneme/`
- DTO: `FolderRequest`, `FolderResponse(extId, parentExtId, path, name, createdAt)`
- 사용자 인증: ApiKeyAuthenticationToken 또는 OAuth2User principal
