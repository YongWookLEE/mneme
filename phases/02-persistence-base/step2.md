# Step 2: flyway-v1-init-schema

> Task 2 of phase 02. `V1__init.sql` 작성 + Testcontainers 마이그레이션 검증 테스트. 본 step의 산출물은 (1) Flyway 마이그레이션 파일과 (2) "pgvector 컨테이너 띄우고 V1 적용 → 13개 테이블이 모두 존재"를 검증하는 통합 테스트 1개.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "데이터 모델", "마이그레이션 정책"
- `docs/ADR.md` ADR-002, ADR-014
- `phases/02-persistence-base/step1.md`

## 작업

### 2.1 `backend/src/main/resources/db/migration/V1__init.sql`

ARCHITECTURE.md의 13개 테이블 + pgvector/pg_trgm 확장 + 인덱스 + tsv 트리거를 한 마이그레이션에 담는다. UUID v7은 애플리케이션에서 생성하므로 DB default는 두지 않는다.

### 2.2 검증 방식 (Testcontainers 보류, 수동 verify 채택)

**TODO**: Testcontainers 1.20.2 + Docker Desktop 29.x 조합에서 `DockerClientProviderStrategy`가 `Info` 응답을 잘못 파싱해 `BadRequestException`을 던지는 호환성 이슈가 확인됨. JUnit 통합 테스트는 phase 02에서는 작성하지 않고, **마이그레이션 검증은 docker compose의 postgres에 직접 V1을 적용한 후 스키마를 쿼리**하는 방식으로 한 step에 한해 대체한다. Testcontainers는 phase 05/08에서 도커-자바 라이브러리 업그레이드와 함께 재도입.

검증 수동 절차:

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml --env-file deploy/.env up -d postgres
docker cp backend/src/main/resources/db/migration/V1__init.sql mneme-postgres-1:/tmp/V1.sql
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml exec -T postgres psql -U mneme -d mneme -f /tmp/V1.sql
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml exec -T postgres psql -U mneme -d mneme -c "
SELECT 'tables: ' || count(*) FROM information_schema.tables WHERE table_schema='public';
SELECT 'extensions: ' || string_agg(extname, ',') FROM pg_extension WHERE extname IN ('vector','pg_trgm');
SELECT 'embedding index: ' || count(*) FROM pg_indexes WHERE indexname='memories_embedding_idx';
SELECT 'tsv trigger: ' || count(*) FROM pg_trigger WHERE tgname='memories_tsv_update';
"
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml down
```

기대 결과:
- `tables: 13`
- `extensions: vector,pg_trgm`
- `embedding index: 1`
- `tsv trigger: 1`

### 2.3 검증

```bash
./gradlew :backend:test :backend:ktlintCheck
```

기존 smoke test 그대로 통과. 새 IT는 위 보류 사유로 작성 안 함.

## Acceptance Criteria

```bash
./gradlew :backend:test
./gradlew :backend:ktlintCheck
```

둘 다 `BUILD SUCCESSFUL`, `FlywayMigrationIT` 1개 PASSED.

## 검증 절차

1. Acceptance 명령 통과.
2. `V1__init.sql`의 테이블 수와 ARCHITECTURE.md "데이터 모델" 절의 테이블 수가 일치(13개 + review_items 후속, total 13).
3. 인덱스·트리거 정의 누락 없음.
4. 성공 시 phases/02-persistence-base/index.json step 2 completed.

## 금지사항

- `DROP TABLE` / `DROP INDEX` 포함 금지. **이유: forward-only**.
- rollback 마이그레이션 작성 금지. **이유: ADR-014**.
- application-test.yml에서 Flyway를 켜지 마라. **이유: smoke test는 H2 사용. 통합 테스트는 별도 @ActiveProfiles + DynamicPropertySource로 운영 프로파일 흉내**.
