package com.mneme.mcp

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ThreadLocalAccessor
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Hooks

/**
 * MCP 도구 실행 시 Reactor 스케줄러로 `SecurityContext`를 전파한다.
 *
 * Spring AI MCP server는 Reactor 비동기 파이프라인 위에서 동작한다. `WebMvcSseServerTransportProvider`가
 * 서블릿 스레드에서 메시지를 받고 `Sink`에 적재하면 `boundedElastic` 스케줄러가 도구를 호출한다. 이 과정에서
 * 서블릿 스레드의 `SecurityContextHolder`(ThreadLocal)는 자연 소실된다.
 *
 * Micrometer Context Propagation API에 `SecurityContext` ThreadLocal accessor를 등록하고 Reactor의
 * `Hooks.enableAutomaticContextPropagation()`을 켜면, Reactor가 subscriber 스레드의 컨텍스트를
 * 워커 스레드로 자동 복원한다. 결과적으로 도구 메서드에서 `SecurityContextHolder.getContext().authentication`이
 * 원 요청과 동일하게 보인다.
 *
 * @author Mneme
 * @since phase 09
 */
@Configuration
class McpContextPropagationConfig {
    /** 컨텍스트 propagation 초기화. */
    @PostConstruct
    fun init() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(SecurityContextThreadLocalAccessor())
        Hooks.enableAutomaticContextPropagation()
    }

    /**
     * Spring Security 6의 `SecurityContext` ThreadLocal accessor.
     *
     * Micrometer가 reactive context와 SecurityContextHolder 사이를 자동 복사·복원한다.
     */
    private class SecurityContextThreadLocalAccessor : ThreadLocalAccessor<SecurityContext> {
        override fun key(): Any = KEY

        override fun getValue(): SecurityContext? {
            val ctx = SecurityContextHolder.getContext()
            return if (ctx.authentication != null) ctx else null
        }

        override fun setValue(value: SecurityContext) {
            SecurityContextHolder.setContext(value)
        }

        override fun setValue() {
            SecurityContextHolder.clearContext()
        }

        companion object {
            const val KEY = "mneme.security.context"
        }
    }
}
