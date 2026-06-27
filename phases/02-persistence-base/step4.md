# Step 4: domain-entities

> Task 4 of phase 02. MVP 핵심 4개 JPA 엔티티(User, Folder, Memory, MemoryLink) + 보조 3개(ApiKey, Tag, MemoryTag)를 작성한다. UUID PK는 application 생성(이 step 3의 IdFactory에서). 낙관적 락은 `updated_at`(Memory만) 대신 `@Version` 필드로 단순화. embedding/tsvector 같은 native PostgreSQL 타입은 본 step에서 매핑하지 않고 `@Transient` 또는 native query에서만 다룬다(phase 06 LLM 도입 시 구현).

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "데이터 모델", "동시성"
- `docs/ADR.md` ADR-013(폴더 1:N + 태그 N:M), ADR-016(낙관적 락)
- `backend/src/main/resources/db/migration/V1__init.sql`

## 작업

### 4.1 패키지 위치

`com.mneme.memory` 아래에 도메인 엔티티 + 리포지토리. 별도의 `domain`/`entity` 서브 패키지는 두지 않는다(현 규모에서 과잉). 후속에 모듈이 커지면 분리.

### 4.2 엔티티 작성

- `User`: id, googleSub, email, locale, createdAt, deletedAt
- `Folder`: id, userId, parentId(nullable), path, name, createdAt
- `Memory`: id, userId, folderId, title, content, summary(nullable), sourceUri(nullable), byteSize, modelVersion, archivedAt(nullable), createdAt, updatedAt, version(`@Version` Long)
- `MemoryLink`: id, userId, sourceId, targetId(nullable, 깨진 링크), targetLabel, kind, createdAt
- `ApiKey`: id, userId, name, keyHash(ByteArray), prefix, lastUsedAt, revokedAt, createdAt
- `Tag`: id, userId, name
- `MemoryTag`: composite key (memoryId, tagId)

본 step에서는 **embedding / tsvector / inet / jsonb / text[]** 는 매핑하지 않는다(phase 05/06에서 도입). `oauth_clients`/`oauth_tokens`/`sessions`/`memory_versions`/`audit_events`/`usage_daily`는 본 step에서 엔티티 생성하지 않고 phase 03/08에서 작성.

### 4.3 컴파일 검증

```bash
./gradlew :backend:build -x test :backend:ktlintCheck
```

`BUILD SUCCESSFUL`. 본 step 4에서는 통합 테스트가 없으므로 기존 smoke test가 깨지지 않으면 충분.

## Acceptance Criteria

```bash
./gradlew :backend:build :backend:ktlintCheck
```

둘 다 `BUILD SUCCESSFUL`. 기존 테스트(스모크 + id 모듈) 모두 PASSED.

## 검증 절차

1. Acceptance 명령 통과.
2. 7개 엔티티 클래스가 `com.mneme.memory` 또는 `com.mneme.auth`(ApiKey, User만) 아래 위치.
3. 모든 클래스/필드에 한국어 KDoc.
4. `@Version`은 Memory에만 부여(낙관적 락 ADR-016 대상이 Memory).
5. 성공 시 index.json step 4 completed.

## 금지사항

- `@OneToMany`/`@ManyToOne` 양방향 매핑 사용 금지. **이유: user_id 강제 시그니처와 충돌. 명시적 `userId: UUID`로만**.
- 엔티티에 `@Service`/`@Component` 부착 금지. **이유: 엔티티는 데이터**.
- Lombok 추가 금지. **이유: Kotlin data class면 충분**.
- embedding/tsvector 컬럼을 ByteArray로 매핑 금지. **이유: PGobject 또는 native query로 phase 05~06에 도입**.
