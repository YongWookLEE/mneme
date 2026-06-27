# Step 2: backend-smoke-test

> Task 2 of phase 01. 백엔드 컨텍스트가 부팅되고 `/actuator/health`가 200 + `{"status":"UP"}`를 반환함을 자동 회귀로 잡는다. TDD 원칙대로 "테스트가 먼저 실패하는 모습"을 보고, 이미 통과하는 환경이면 해당 step에서 의도된 보호 회귀로 본다.

## 읽어야 할 파일

- `phases/01-project-skeleton/step1.md` (이 step의 직전 산출물)
- `backend/src/main/kotlin/com/mneme/MnemeApplication.kt`
- `backend/src/main/resources/application.yml`
- `CLAUDE.md` "개발 프로세스" (TDD 의무)

## 작업

### 2.1 테스트용 프로파일 yml

`backend/src/test/resources/application-test.yml`:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

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

### 2.2 스모크 테스트 작성 (먼저 실패해야 함)

`backend/src/test/kotlin/com/mneme/MnemeApplicationSmokeTest.kt`:

```kotlin
package com.mneme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * Mneme 백엔드 스모크 테스트.
 *
 * Spring Boot 컨텍스트가 로드되고 Actuator 헬스 엔드포인트가 UP을 반환하는지 검증한다.
 * 이후 phase에서 통합 테스트가 추가될 때 동일한 패턴(@SpringBootTest + TestRestTemplate)을 재사용한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MnemeApplicationSmokeTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    /**
     * 컨텍스트 로드 확인. 빈 본문도 충분하다 — 메서드가 실행됐다는 사실이 곧 컨텍스트 부트 성공.
     */
    @Test
    fun `Spring 컨텍스트가 정상 로드된다`() {
        // 컨텍스트 로드 실패 시 SpringBootTest가 예외를 던진다
    }

    /**
     * Actuator 헬스 엔드포인트가 200 + body에 "UP"을 포함해야 한다.
     */
    @Test
    fun `actuator health 가 UP 을 반환한다`() {
        val response = restTemplate.getForEntity("/actuator/health", String::class.java)

        response.statusCode shouldBe HttpStatus.OK
        (response.body ?: "").contains("\"status\":\"UP\"") shouldBe true
    }
}
```

### 2.3 테스트 실행 → 통과 확인

```bash
./gradlew :backend:test --tests "com.mneme.MnemeApplicationSmokeTest" -i
```

기대 출력 (마지막 라인):

```
BUILD SUCCESSFUL in Xs
3 actionable tasks: 1 executed, 2 up-to-date
```

테스트 로그에는:

```
MnemeApplicationSmokeTest > Spring 컨텍스트가 정상 로드된다 PASSED
MnemeApplicationSmokeTest > actuator health 가 UP 을 반환한다 PASSED
```

### 2.4 전체 테스트 + ktlint 확인

```bash
./gradlew :backend:test :backend:ktlintCheck
```

둘 다 `BUILD SUCCESSFUL`.

### 2.5 커밋

```bash
git add backend/src/test
git commit -m "test(backend): add application smoke test for context + health

- @SpringBootTest(webEnvironment=RANDOM_PORT) + TestRestTemplate
- Actuator health 엔드포인트 UP 검증
- application-test.yml 로 DataSource 자동설정 제외 유지

Refs: phase 01 step 2"
```

## Acceptance Criteria

```bash
./gradlew :backend:test
./gradlew :backend:ktlintCheck
```

둘 다 `BUILD SUCCESSFUL`. 테스트 결과에 `MnemeApplicationSmokeTest` 2개 메서드 모두 PASSED.

## 검증 절차

1. Acceptance Criteria 명령 모두 통과.
2. `MnemeApplicationSmokeTest.kt`에 한국어 KDoc이 클래스 + 모든 `@Test` 메서드에 있는지 확인.
3. 테스트가 외부 DB/OpenAI/Google 호출을 시도하지 않는지(stdout 로그에 connection refused 없음) 확인.
4. 성공 시 `phases/01-project-skeleton/index.json`의 step 2 갱신:
   ```json
   { "step": 2, "name": "backend-smoke-test", "status": "completed", "summary": "context-load + actuator/health UP 회귀 테스트 1개 클래스 2개 케이스 PASSED" }
   ```

## 금지사항

- 컨트롤러 단위 테스트를 추가하지 마라. **이유: 컨트롤러 자체가 아직 없음**.
- WireMock으로 OpenAI/Google mock을 만들지 마라. **이유: 의존성도 도입 전, phase 06에서**.
- `@MockBean` 남발 금지. **이유: 이 step은 컨텍스트가 실제로 부팅하는지 검증하는 게 목적**.
- 테스트에 `Thread.sleep` 사용 금지. **이유: TestRestTemplate는 동기. 슬립은 플레이키 원인**.
