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
