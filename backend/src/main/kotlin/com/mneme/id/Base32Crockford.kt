package com.mneme.id

import java.util.UUID

/**
 * Crockford Base32 인코더/디코더.
 *
 * 알파벳: `0-9` + `A-Z` 중 I, L, O, U 제외 (32자).
 * 출력은 소문자, 입력 디코딩은 대소문자 무시 + 혼동 문자 자동 매핑(O→0, I/L→1).
 *
 * UUID(128비트) → 26자 문자열 (130비트 표현, 상위 2비트 0 padding).
 */
object Base32Crockford {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val ENCODED_LEN = 26

    /**
     * UUID를 Crockford base32 26자 소문자 문자열로 인코딩.
     */
    fun encode(uuid: UUID): String {
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        // 128비트를 5비트 단위로 26개. 상위 2비트는 0 padding.
        val out = CharArray(ENCODED_LEN)
        // 각 5비트 청크를 추출 — 최상위부터
        for (i in 0 until ENCODED_LEN) {
            val bitOffset = 130 - 5 - (i * 5) // 최상위 청크부터
            val v = extractBits(msb, lsb, bitOffset, 5)
            out[i] = ALPHABET[v.toInt()].lowercaseChar()
        }
        return String(out)
    }

    /**
     * base32 26자 문자열(대소문자 무관, 혼동 문자 자동 매핑)을 UUID로 디코딩.
     *
     * @throws IllegalArgumentException 길이 불일치 또는 알파벳 외 문자
     */
    fun decode(text: String): UUID {
        require(text.length == ENCODED_LEN) { "base32 길이는 $ENCODED_LEN 자여야 합니다 (입력: ${text.length})" }

        var msb = 0L
        var lsb = 0L
        for (i in 0 until ENCODED_LEN) {
            val c = mapChar(text[i])
            val v = ALPHABET.indexOf(c).toLong()
            require(v >= 0) { "잘못된 base32 문자: '${text[i]}'" }

            val bitOffset = 130 - 5 - (i * 5)
            val (newMsb, newLsb) = setBits(msb, lsb, v, bitOffset, 5)
            msb = newMsb
            lsb = newLsb
        }
        return UUID(msb, lsb)
    }

    /**
     * Crockford 혼동 문자 매핑: 대문자화 후 `O→0`, `I/L→1`.
     */
    private fun mapChar(c: Char): Char {
        val u = c.uppercaseChar()
        return when (u) {
            'O' -> '0'
            'I', 'L' -> '1'
            'U' -> throw IllegalArgumentException("'U'는 Crockford 알파벳에 없습니다")
            else -> u
        }
    }

    /**
     * msb+lsb 128비트 공간에서 `bitOffset`에 위치한 `width` 비트(최대 5)를 추출.
     * bitOffset은 LSB 기준(0이 가장 낮은 비트, 127이 가장 높은 비트).
     */
    private fun extractBits(
        msb: Long,
        lsb: Long,
        bitOffset: Int,
        width: Int,
    ): Long {
        val mask = (1L shl width) - 1L
        // bitOffset이 음수이면 lower bits to slot 0
        return if (bitOffset >= 64) {
            // msb 쪽
            val shift = bitOffset - 64
            (msb ushr shift) and mask
        } else if (bitOffset + width <= 64) {
            // lsb 안에서 모두
            (lsb ushr bitOffset) and mask
        } else {
            // 두 long 경계에 걸침 (UUID v7에서는 width=5라 가능)
            val lowPart = lsb ushr bitOffset
            val highPart = msb shl (64 - bitOffset)
            (lowPart or highPart) and mask
        }
    }

    private fun setBits(
        msb: Long,
        lsb: Long,
        value: Long,
        bitOffset: Int,
        width: Int,
    ): Pair<Long, Long> {
        val mask = (1L shl width) - 1L
        val v = value and mask
        return if (bitOffset >= 64) {
            val shift = bitOffset - 64
            Pair(msb or (v shl shift), lsb)
        } else if (bitOffset + width <= 64) {
            Pair(msb, lsb or (v shl bitOffset))
        } else {
            val lowShift = bitOffset
            val highShift = 64 - bitOffset
            Pair(msb or (v ushr highShift), lsb or (v shl lowShift))
        }
    }
}
