package com.mneme.mcp

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MCP 도구 콜백 공급자 설정.
 *
 * Spring AI MCP server starter는 등록된 [ToolCallbackProvider] 빈을 자동 발견하여
 * `tools/list`·`tools/call` JSON-RPC 요청에 응답한다. [MnemeTools]의 모든 `@Tool` 메서드가
 * MCP 도구로 등록된다.
 *
 * @author Mneme
 * @since phase 09
 */
@Configuration
class McpToolsConfig {
    /** MnemeTools의 메서드 기반 도구 콜백 공급자. */
    @Bean
    fun mnemeToolCallbackProvider(tools: MnemeTools): ToolCallbackProvider = MethodToolCallbackProvider.builder().toolObjects(tools).build()
}
