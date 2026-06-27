# Step 5: repositories-and-isolation-base

> Task 5 of phase 02. Spring Data JPA 리포지토리 인터페이스를 만들되 **사용자 데이터 격리 CRITICAL 규칙**을 시그니처 차원에서 강제한다. 모든 조회/수정 메서드는 첫 인자 `userId: UUID`를 명시한다. `JpaRepository`를 그대로 노출하지 않고 도메인별 인터페이스로 감싼다.
>
> Testcontainers는 phase 02에서 보류된 상태(step 2 참고)이므로 본 step의 격리 회귀는 docker compose postgres 위에서 매뉴얼 검증한다. 본격적인 JUnit `IsolationRegressionTest`는 phase 05/08에서 도입.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "데이터 격리 (CRITICAL)"
- `docs/ADR.md` ADR-009 (격리 코드+테스트 이중 강제)
- `backend/src/main/kotlin/com/mneme/memory/*.kt`, `auth/*.kt`

## 작업

### 5.1 리포지토리 인터페이스

각 엔티티별 도메인 리포지토리. JpaRepository를 상속하되 시그니처 자체에 userId가 들어가도록 강제:

- `UserRepository` (사용자 본인은 user_id 강제와 별개. googleSub 조회용)
- `FolderRepository`: `findByUserIdAndId(userId, id)`, `findByUserIdAndPath(userId, path)`, `findAllByUserIdAndParentId(userId, parentId)`
- `MemoryRepository`: `findByUserIdAndId(userId, id)`, `findAllByUserIdAndFolderId(userId, folderId)`, `findAllByUserIdAndArchivedAtIsNull(userId)`
- `MemoryLinkRepository`: `findAllByUserIdAndSourceId(userId, sourceId)`, `findAllByUserIdAndTargetId(userId, targetId)`, `deleteAllByUserIdAndSourceId(userId, sourceId)`
- `TagRepository`: `findByUserIdAndName(userId, name)`, `findAllByUserId(userId)`
- `MemoryTagRepository`: `findAllByIdMemoryId(memoryId)`, `deleteAllByIdMemoryId(memoryId)` — userId는 join에서 강제
- `ApiKeyRepository`: `findAllByUserIdAndRevokedAtIsNull(userId)`, `findByPrefixAndRevokedAtIsNull(prefix)` (이건 인증 검증용, userId 없이 prefix로 후보 조회 후 hash 비교에서 keyHash + userId 후속 검증)

### 5.2 ktlint 함수 시그니처 룰

ktlint 12.x의 `function-signature` 규칙으로 인해 멀티 파라미터 함수는 각 인자가 새 줄에 위치해야 한다. 인터페이스 메서드도 마찬가지. step 3에서 학습한 패턴 그대로.

### 5.3 검증

```bash
./gradlew :backend:build :backend:test :backend:ktlintCheck
```

기존 테스트가 깨지지 않아야 함. 새 리포지토리는 별도 단위 테스트 없이 컴파일 + Spring Boot 빈 등록만 검증(smoke test의 context-load 통과로 갈음).

수동 격리 확인:

```bash
# 1. compose postgres 띄움
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml --env-file deploy/.env up -d postgres

# 2. V1 적용
docker cp backend/src/main/resources/db/migration/V1__init.sql mneme-postgres-1:/tmp/V1.sql
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml exec -T postgres psql -U mneme -d mneme -f /tmp/V1.sql

# 3. 두 사용자 + 메모리 더미 insert 후 SELECT WHERE user_id = ... 격리 확인 (수동)
# → 본 step에서는 스키마 + 리포지토리 시그니처만 보장. SQL 행위 회귀는 phase 05/08.

# 4. compose 종료
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml down
```

## Acceptance Criteria

```bash
./gradlew :backend:build :backend:test :backend:ktlintCheck
```

세 명령 모두 `BUILD SUCCESSFUL`.

## 검증 절차

1. Acceptance 명령 통과.
2. 모든 리포지토리 인터페이스 메서드의 첫 인자가 `userId: UUID` (단, prefix 인증 조회 같은 예외는 KDoc에 사유 명시).
3. 한국어 KDoc — 인터페이스 + 모든 메서드.
4. 성공 시 index.json step 5 completed.

## 금지사항

- `JpaRepository<Entity, UUID>`만 노출하고 메서드 추가 없이 끝내지 마라. **이유: `findById(id)`가 격리 우회를 허용**.
- 리포지토리 메서드에 `@Query` 직접 SQL 작성 시 `WHERE user_id` 누락 금지. **이유: 격리 강제**.
- 본 step에서 통합 테스트 추가하지 마라. **이유: Testcontainers 보류 중. phase 05/08에서 도입**.
