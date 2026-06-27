package com.mneme.auth

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

/**
 * ApiKeyGenerator 단위 테스트.
 */
class ApiKeyGeneratorTest {
    private val gen = ApiKeyGenerator()

    @Test
    fun `평문 키는 mn_ prefix 로 시작한다`() {
        val k = gen.generate()
        k.plaintext shouldStartWith "mn_"
    }

    @Test
    fun `식별자는 평문 앞 8 자와 같다`() {
        val k = gen.generate()
        k.identifier shouldBe k.plaintext.take(8)
    }

    @Test
    fun `verify 는 본인 평문에 true 를 반환한다`() {
        val k = gen.generate()
        gen.verify(k.plaintext, k.hash) shouldBe true
    }

    @Test
    fun `verify 는 다른 평문에 false 를 반환한다`() {
        val a = gen.generate()
        val b = gen.generate()
        gen.verify(a.plaintext, b.hash) shouldBe false
    }

    @Test
    fun `100 개 생성 시 평문 중복이 없다`() {
        val set = mutableSetOf<String>()
        repeat(100) {
            set += gen.generate().plaintext
        }
        set.size shouldBe 100
    }
}
