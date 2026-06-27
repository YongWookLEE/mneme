# Step 1: persistence-deps-and-config

> Task 1 of phase 02. JPA·PostgreSQL·Flyway·pgvector·HikariCP를 backend에 도입한다. `application.yml`의 DataSource autoconfigure 제외를 풀고, 로컬·운영·테스트 프로파일별로 DB 접속 정보를 구성한다. Flyway 마이그레이션은 빈 `db/migration/`(이미 `.gitkeep`만 있음)에서 시작 — V1은 step 2.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "마이그레이션 정책 (Flyway)", "트랜잭션 경계"
- `docs/ADR.md` ADR-002(Postgres+pgvector), ADR-014(Flyway forward-only)
- `backend/build.gradle.kts`, `backend/src/main/resources/application*.yml`

## 작업

### 1.1 `backend/build.gradle.kts` 의존성 추가

기존 `dependencies` 블록에 추가:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
implementation("org.postgresql:postgresql")
implementation("com.pgvector:pgvector:0.1.6")
```

`testImplementation`에 추가:

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:junit-jupiter:1.20.2")
testImplementation("org.testcontainers:postgresql:1.20.2")
```

### 1.2 `backend/src/main/resources/application.yml`

- `spring.autoconfigure.exclude` 두 줄 삭제
- DataSource / JPA / Flyway / Hikari 추가

```yaml
spring:
  application:
    name: mneme
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jackson:
    default-property-inclusion: NON_NULL
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
  datasource:
    url: ${MNEME_DB_URL:jdbc:postgresql://localhost:5432/mneme}
    username: ${MNEME_DB_USER:mneme}
    password: ${MNEME_DB_PASSWORD:}
    hikari:
      pool-name: mneme-hikari
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 30000
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
      hibernate.format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
```

server / management / mneme 블록은 그대로 유지.

### 1.3 `application-local.yml` / `application-prod.yml`

local/prod 프로파일에 DataSource 별 차이가 없으면 그대로. Hibernate SQL 로깅은 local에서만:

`application-local.yml`에 추가:

```yaml
spring:
  jpa:
    properties:
      hibernate.show_sql: false
logging:
  level:
    root: INFO
    com.mneme: DEBUG
    org.hibernate.SQL: INFO
```

### 1.4 `backend/src/test/resources/application-test.yml`

기존 autoconfigure exclude를 제거하고, JPA·Flyway 활성화 + DataSource는 Testcontainers DynamicPropertySource 또는 명시적 비활성화로 처리. 일단 본 step에서는 **JPA를 유지하되 DataSource를 통합 테스트에서 주입**한다.

이 step에서는 `MnemeApplicationSmokeTest`가 깨지지 않게만 보장. Testcontainers 진입은 step 2.

```yaml
spring:
  flyway:
    enabled: false   # smoke test에서는 마이그레이션 적용 안 함
  datasource:
    url: jdbc:h2:mem:smoke
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: none

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true

mneme:
  base-url: http://localhost
  frontend-origin: http://localhost:5173
```

> H2를 임시 introduce하는 이유: smoke test는 컨텍스트 + Actuator만 검증. Testcontainers 띄우는 비용을 step 1에서 강요하지 않음. step 2에서 마이그레이션·통합 테스트는 Testcontainers로 명시적 검증.
> 단 운영 코드 build에 H2 의존성을 추가하지 않는다(`testRuntimeOnly`로 제한).

`backend/build.gradle.kts` 에 추가:

```kotlin
testRuntimeOnly("com.h2database:h2:2.3.232")
```

### 1.5 검증

```bash
docker run --rm -v "$PWD":/app -w /app -v gradle-cache:/home/gradle/.gradle gradle:8.7-jdk21 \
  ./gradlew :backend:build -x test :backend:ktlintCheck
```

기대: `BUILD SUCCESSFUL`. (test는 step 2에서 다룸. 단 smoke test는 깨지면 안 됨 — 다음 명령으로 확인)

```bash
docker run --rm -v "$PWD":/app -w /app -v gradle-cache:/home/gradle/.gradle gradle:8.7-jdk21 \
  ./gradlew :backend:test
```

`MnemeApplicationSmokeTest` 2 케이스 PASSED.

## Acceptance Criteria

```bash
./gradlew :backend:build -x test
./gradlew :backend:test
./gradlew :backend:ktlintCheck
```

세 명령 모두 `BUILD SUCCESSFUL`.

## 검증 절차

1. Acceptance 명령 모두 통과.
2. `application.yml`에서 `spring.autoconfigure.exclude` 두 줄이 사라졌는지.
3. `build.gradle.kts`에 추가된 의존성 5개(prod) + 3개(test) + H2(testRuntimeOnly).
4. 성공 시 `phases/02-persistence-base/index.json`의 step 1 `completed` + `summary`.

## 금지사항

- 운영 의존성에 H2 추가 금지. **이유: 잘못된 환경에서 H2가 잡힘**.
- `ddl-auto: update` 같은 자동 DDL 금지. **이유: Flyway forward-only 원칙 위반**.
- Flyway 비활성화(`enabled: false`)를 운영 프로파일에 두지 마라. **이유: 마이그레이션 누락 위험**.
