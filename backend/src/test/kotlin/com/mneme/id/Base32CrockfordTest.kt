package com.mneme.id

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Crockford Base32 인코더/디코더 단위 테스트.
 */
class Base32CrockfordTest {
    /**
     * 랜덤 UUID v7 100개를 인코딩 → 디코딩 → 원본과 일치.
     */
    @Test
    fun `UUID round-trip 이 성립한다`() {
        repeat(100) {
            val original = UuidV7.newId()
            val encoded = Base32Crockford.encode(original)
            encoded.length shouldBe 26
            val decoded = Base32Crockford.decode(encoded)
            decoded shouldBe original
        }
    }

    /**
     * 인코딩 출력은 항상 26자 소문자.
     */
    @Test
    fun `인코딩 결과는 26 자 소문자이다`() {
        val id = UuidV7.newId()
        val encoded = Base32Crockford.encode(id)
        encoded.length shouldBe 26
        (encoded == encoded.lowercase()) shouldBe true
    }

    /**
     * 대문자 입력도 디코딩 가능(round-trip).
     */
    @Test
    fun `디코딩은 대소문자를 무시한다`() {
        val id = UuidV7.newId()
        val encoded = Base32Crockford.encode(id)
        Base32Crockford.decode(encoded.uppercase()) shouldBe id
    }

    /**
     * Crockford 혼동 문자 매핑: O→0, I/L→1.
     */
    @Test
    fun `혼동 문자 매핑이 동작한다`() {
        val id = UuidV7.newId()
        val encoded = Base32Crockford.encode(id)
        // '0'은 base32 알파벳에 항상 있을 수도 없을 수도 있어 강제 치환 테스트는 별도로
        val swapped = encoded.replace('0', 'O').replace('1', 'I')
        Base32Crockford.decode(swapped) shouldBe id
    }

    @Test
    fun `잘못된 길이는 예외를 던진다`() {
        shouldThrow<IllegalArgumentException> { Base32Crockford.decode("short") }
        shouldThrow<IllegalArgumentException> { Base32Crockford.decode("a".repeat(27)) }
    }

    @Test
    fun `U 문자는 거부된다`() {
        val invalid = "u".repeat(26)
        shouldThrow<IllegalArgumentException> { Base32Crockford.decode(invalid) }
    }
}
