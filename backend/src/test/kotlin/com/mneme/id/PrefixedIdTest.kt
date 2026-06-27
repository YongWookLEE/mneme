package com.mneme.id

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

/**
 * PrefixedId 직렬화·역직렬화 단위 테스트.
 */
class PrefixedIdTest {
    @Test
    fun `format 결과는 prefix 와 base32 26 자 를 언더스코어로 결합한다`() {
        val id = PrefixedId(PrefixedId.Prefix.MEMORY, UuidV7.newId())
        val ext = id.format()
        ext shouldStartWith "mem_"
        ext.length shouldBe 4 + 26
    }

    @Test
    fun `format parse round-trip 이 성립한다`() {
        val original = PrefixedId(PrefixedId.Prefix.MEMORY, UuidV7.newId())
        val parsed = PrefixedId.parse(original.format(), PrefixedId.Prefix.MEMORY)
        parsed shouldBe original
    }

    @Test
    fun `prefix 불일치는 예외를 던진다`() {
        val id = PrefixedId(PrefixedId.Prefix.MEMORY, UuidV7.newId()).format()
        shouldThrow<IllegalArgumentException> {
            PrefixedId.parse(id, PrefixedId.Prefix.FOLDER)
        }
    }

    @Test
    fun `알 수 없는 prefix 는 생성자에서 예외를 던진다`() {
        shouldThrow<IllegalArgumentException> {
            PrefixedId("xxx", UuidV7.newId())
        }
    }

    @Test
    fun `구분자가 없는 입력은 예외를 던진다`() {
        shouldThrow<IllegalArgumentException> {
            PrefixedId.parse("memabcdefghijklmnopqrstuvwxyz", PrefixedId.Prefix.MEMORY)
        }
    }
}
