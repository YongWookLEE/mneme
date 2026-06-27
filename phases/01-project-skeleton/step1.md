# Step 1: gradle-root-and-backend-skeleton

> Task 1 of phase 01. Gradle 멀티프로젝트 루트와 backend Kotlin Spring Boot 모듈을 부트스트랩한다. 도메인 코드는 없고, `bootRun` 시 빈 컨텍스트 + `/actuator/health`만 노출되면 성공.

## 읽어야 할 파일

- `CLAUDE.md` (특히 "기술 스택", "아키텍처 규칙", "명령어")
- `docs/ARCHITECTURE.md` (디렉터리 구조 + 모듈 경계 + HikariCP 설정)
- `docs/ADR.md` ADR-001(Kotlin+Spring Boot), ADR-008(Compose)
- `phases/01-project-skeleton/plan.md` (이 step의 위치)

## 작업

### 1.1 Gradle wrapper 8.7 배치

로컬에 gradle이 설치되어 있으면:

```bash
cd /Users/lyw/Documents/self/workspace/mneme
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

없으면 Docker로 한 번만 생성:

```bash
docker run --rm -v "$PWD":/app -w /app gradle:8.7-jdk21 gradle wrapper --gradle-version 8.7 --distribution-type bin
```

산출: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.{jar,properties}`. 모두 커밋 대상.

### 1.2 `settings.gradle.kts`

```kotlin
rootProject.name = "mneme"

include("backend")
```

### 1.3 루트 `build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

allprojects {
    group = "com.mneme"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
```

### 1.4 `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
```

### 1.5 `backend/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    version.set("1.3.1")
    filter {
        exclude { it.file.path.contains("build/") }
    }
}
```

### 1.6 `backend/src/main/kotlin/com/mneme/MnemeApplication.kt`

```kotlin
package com.mneme

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Mneme 백엔드 진입점.
 *
 * Spring Boot 컨텍스트를 구동하고 REST·MCP·인증·LLM·검색 모듈을 로드한다.
 * Phase 01에서는 도메인 코드 없이 Actuator 헬스 엔드포인트만 노출한다.
 */
@SpringBootApplication
class MnemeApplication

/**
 * JVM 진입점. Spring Boot 컨텍스트를 시작한다.
 *
 * @param args 커맨드라인 인자 (Spring 환경에 그대로 전달)
 */
fun main(args: Array<String>) {
    runApplication<MnemeApplication>(*args)
}
```

### 1.7 모듈 디렉터리 `.gitkeep` (구조 선점)

`docs/ARCHITECTURE.md`의 백엔드 패키지 구조를 사전 생성한다. 빈 디렉터리는 git이 추적하지 않으므로 `.gitkeep`을 둔다:

```bash
for pkg in api mcp auth memory llm search export security observability notification persistence id config; do
  mkdir -p "backend/src/main/kotlin/com/mneme/$pkg"
  touch "backend/src/main/kotlin/com/mneme/$pkg/.gitkeep"
done
mkdir -p backend/src/main/resources/db/migration backend/src/main/resources/i18n
touch backend/src/main/resources/db/migration/.gitkeep
```

### 1.8 `backend/src/main/resources/application.yml`

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
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

server:
  port: 8080
  shutdown: graceful
  forward-headers-strategy: framework

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true

mneme:
  base-url: ${MNEME_BASE_URL:http://localhost:8080}
  frontend-origin: ${MNEME_FRONTEND_ORIGIN:http://localhost:5173}
```

> 이 phase에서는 DataSource를 의도적으로 제외한다. Phase 02에서 Flyway·HikariCP·pgvector를 도입하면서 위 `exclude` 두 줄을 지운다.

### 1.9 `backend/src/main/resources/application-local.yml`

```yaml
logging:
  level:
    root: INFO
    com.mneme: DEBUG
```

### 1.10 `backend/src/main/resources/application-prod.yml`

```yaml
server:
  forward-headers-strategy: framework

logging:
  level:
    root: WARN
    com.mneme: INFO
```

### 1.11 `backend/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <property name="LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

> 구조화 JSON 로그는 phase 14(observability)에서 logstash-encoder로 교체.

### 1.12 `.gitignore` 갱신 (이미 존재. backend 빌드 산출물 항목이 있는지만 확인)

존재해야 하는 항목 (없으면 추가):

```gitignore
# Gradle
.gradle/
build/
out/
**/build/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store

# 환경/비밀
.env
deploy/.env
```

### 1.13 빌드·린트 검증

```bash
./gradlew :backend:build -x test
./gradlew :backend:ktlintCheck
```

기대 출력: 두 명령 모두 `BUILD SUCCESSFUL` 로 종료.

만약 ktlint가 첫 실행에서 포맷 위반을 잡으면:

```bash
./gradlew :backend:ktlintFormat
./gradlew :backend:ktlintCheck
```

### 1.14 커밋

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/ \
        backend/ .gitignore
git commit -m "chore(backend): bootstrap kotlin spring boot skeleton

- gradle 8.7 wrapper + kotlin 1.9.25 + spring boot 3.3.4
- com.mneme 패키지 스캐폴딩(.gitkeep)
- application.yml + local/prod 프로파일 + 로그백 기본 설정
- 데이터소스 자동설정 제외(phase 02에서 도입)
- ktlint 12.1.1 통합

Refs: ADR-001, docs/ARCHITECTURE.md 디렉터리 구조"
```

## Acceptance Criteria

```bash
./gradlew :backend:build -x test
./gradlew :backend:ktlintCheck
```

두 명령 모두 `BUILD SUCCESSFUL`.

추가 수동 검증:

```bash
./gradlew :backend:bootRun &
sleep 15
curl -sf http://localhost:8080/actuator/health
# 기대: {"status":"UP"}
kill %1
```

## 검증 절차

1. Acceptance Criteria의 두 gradle 명령 실행 → 둘 다 성공.
2. `MnemeApplication.kt`에 한국어 KDoc이 함수·클래스에 모두 있는지 확인.
3. `docs/ARCHITECTURE.md`의 백엔드 디렉터리 구조가 `backend/src/main/kotlin/com/mneme/` 아래 그대로 만들어졌는지 확인.
4. `application.yml`이 ADR-005에 명시된 환경변수 prefix `MNEME_*`을 사용하는지 확인.
5. 성공 시 `phases/01-project-skeleton/index.json`의 step 1을 갱신:
   ```json
   { "step": 1, "name": "gradle-root-and-backend-skeleton", "status": "completed", "summary": "gradle 8.7 + spring boot 3.3.4 + kotlin 1.9.25 백엔드 골격, /actuator/health UP" }
   ```
6. 실패 3회 시 `status: error`, `error_message`에 구체적 원인 기록 후 중단.

## 금지사항

- Flyway, JPA, HikariCP, pgvector 의존성을 이 step에서 추가하지 마라. **이유: phase 02 범위**. 추가하면 phase 02 작업과 충돌.
- 컨트롤러를 만들지 마라. **이유: 도메인 정의 전이라 의미 없는 코드**.
- `application.yml`에 OpenAI/Google 키를 하드코딩하지 마라. **이유: CLAUDE.md CRITICAL — 비밀은 환경변수**.
- 기존 `docs/` 문서를 수정하지 마라. **이유: 이 step 범위 밖**.
- Spring Initializr 산출물을 그대로 덮어쓰지 마라. **이유: KDoc 누락 + 디렉터리 구조 불일치**.
