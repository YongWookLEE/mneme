package com.mneme.llm

/**
 * 사용자 본문을 LLM 프롬프트에 안전하게 삽입하기 위한 가드.
 *
 * - 본문을 `<<<USER_CONTENT … END_USER_CONTENT>>>` 펜스 안에 가둔다. 모델이 펜스 안 텍스트를
 *   "지시"가 아닌 "데이터"로 다루도록 시스템 프롬프트에서 명시한다.
 * - 8KB 절단. 그 이상의 본문은 분류·요약 충실도와 토큰 비용 사이 트레이드오프로 잘라낸다.
 * - 펜스 충돌(본문에 같은 마커가 있으면 가드 우회 가능)을 막기 위해 백슬래시 이스케이프.
 */
object PromptGuard {
    private const val MAX_BYTES = 8 * 1024
    private const val OPEN = "<<<USER_CONTENT"
    private const val CLOSE = "END_USER_CONTENT>>>"

    fun fence(userText: String): String {
        val truncated = truncateUtf8(userText, MAX_BYTES)
        val sanitized =
            truncated
                .replace(OPEN, "<<<\\USER_CONTENT")
                .replace(CLOSE, "END_USER_CONTENT\\>>>")
        return "$OPEN\n$sanitized\n$CLOSE"
    }

    private fun truncateUtf8(
        text: String,
        maxBytes: Int,
    ): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return text
        var cut = maxBytes
        while (cut > 0 && (bytes[cut].toInt() and 0xC0) == 0x80) cut--
        return String(bytes, 0, cut, Charsets.UTF_8)
    }
}
