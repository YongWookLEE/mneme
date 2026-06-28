package com.mneme.observability

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class PiiMaskingConverterTest {
    @Test
    fun `mn_ key masked`() {
        val out = PiiMaskingConverter.mask("issued key=mn_LIVETEST20260628000000000000000000000000000")
        out shouldBe "issued key=mn_********"
    }

    @Test
    fun `sk- key masked`() {
        val out = PiiMaskingConverter.mask("OPENAI=sk-abc_XYZ-123")
        out shouldBe "OPENAI=sk-********"
    }

    @Test
    fun `email masked but tld kept`() {
        val out = PiiMaskingConverter.mask("user sdsd1008@gmail.com logged in")
        out shouldBe "user ***@***.com logged in"
    }

    @Test
    fun `multiple patterns coexist`() {
        val out = PiiMaskingConverter.mask("from=test@example.com key=mn_AAAA")
        out shouldNotContain "test@example"
        out shouldNotContain "mn_AAAA"
    }
}
