package com.mneme

import com.mneme.llm.LlmProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Mneme 백엔드 진입점.
 *
 * Spring Boot 컨텍스트를 구동하고 REST·MCP·인증·LLM·검색 모듈을 로드한다.
 * Phase 01에서는 도메인 코드 없이 Actuator 헬스 엔드포인트만 노출한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = [LlmProperties::class])
class MnemeApplication

/**
 * JVM 진입점. Spring Boot 컨텍스트를 시작한다.
 *
 * @param args 커맨드라인 인자 (Spring 환경에 그대로 전달)
 */
fun main(args: Array<String>) {
    runApplication<MnemeApplication>(*args)
}
