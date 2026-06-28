package com.mneme.observability

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * 로그 메시지에서 PII/비밀값 패턴을 자동 마스킹.
 *
 * - `mn_[A-Za-z0-9]+` → `mn_********`
 * - `sk-[A-Za-z0-9_-]+` → `sk-********`
 * - 이메일 → `***@***.<tld>`
 *
 * `logback-spring.xml`에서 `%maskedMsg` 패턴으로 등록.
 */
class PiiMaskingConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String = mask(event.formattedMessage ?: "")

    companion object {
        private val API_KEY = Regex("mn_[A-Za-z0-9]+")
        private val OPENAI_KEY = Regex("sk-[A-Za-z0-9_\\-]+")
        private val EMAIL = Regex("([A-Za-z0-9._%+\\-]+)@([A-Za-z0-9.\\-]+)\\.([A-Za-z]{2,})")

        fun mask(input: String): String {
            var out = API_KEY.replace(input, "mn_********")
            out = OPENAI_KEY.replace(out, "sk-********")
            out = EMAIL.replace(out, "***@***.$3")
            return out
        }
    }
}
