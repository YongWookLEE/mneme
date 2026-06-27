package com.mneme.auth

import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * API 키 생성기.
 *
 * 평문 키 포맷: `mn_<base62-encoded 32 bytes>` — 약 43자.
 * 평문은 발급 응답에 1회만 노출하고 DB에는 sha256 해시(`keyHash` ByteArray)와 앞 8자 식별자(`prefix` TEXT)만 저장한다.
 * prefix는 평문 키의 prefix 8자(즉 `mn_xxxxx`의 앞 8자)이며, 인덱스 partial(`WHERE revoked_at IS NULL`)을 이용해 빠른 후보 조회용.
 */
@Component
class ApiKeyGenerator {
    private val random = SecureRandom()

    /**
     * 새 평문 키와 sha256 해시 + 식별 prefix를 생성한다.
     */
    fun generate(): Generated {
        val bytes = ByteArray(KEY_BYTES)
        random.nextBytes(bytes)
        val plaintextBody = base62Encode(bytes)
        val plaintext = "$PLAIN_PREFIX$plaintextBody"
        val hash = sha256(plaintext)
        val identifier = plaintext.take(IDENTIFIER_LEN)
        return Generated(plaintext = plaintext, hash = hash, identifier = identifier)
    }

    /**
     * 사용자가 제출한 평문 키가 저장된 해시와 일치하는지 검증.
     * 일정 시간 비교(constant-time)는 MessageDigest.isEqual 사용.
     */
    fun verify(
        plaintext: String,
        storedHash: ByteArray,
    ): Boolean {
        val computed = sha256(plaintext)
        return MessageDigest.isEqual(computed, storedHash)
    }

    private fun sha256(text: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(text.toByteArray(Charsets.UTF_8))
    }

    private fun base62Encode(bytes: ByteArray): String {
        // 0을 정수 prefix로 사용해 음수 BigInteger를 피한다
        val padded = ByteArray(bytes.size + 1)
        padded[0] = 0
        System.arraycopy(bytes, 0, padded, 1, bytes.size)
        var n = BigInteger(padded)
        if (n.signum() == 0) return "0"
        val sb = StringBuilder()
        val base = BigInteger.valueOf(62)
        while (n.signum() > 0) {
            val divmod = n.divideAndRemainder(base)
            sb.append(ALPHABET[divmod[1].toInt()])
            n = divmod[0]
        }
        return sb.reverse().toString()
    }

    /**
     * 생성 결과 묶음.
     *
     * @property plaintext 발급 응답에서 1회만 노출할 평문 키
     * @property hash DB에 저장할 sha256 해시
     * @property identifier 인덱스 조회용 앞 8자(평문 prefix 그대로)
     */
    data class Generated(
        val plaintext: String,
        val hash: ByteArray,
        val identifier: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Generated) return false
            return plaintext == other.plaintext && hash.contentEquals(other.hash) && identifier == other.identifier
        }

        override fun hashCode(): Int {
            var result = plaintext.hashCode()
            result = 31 * result + hash.contentHashCode()
            result = 31 * result + identifier.hashCode()
            return result
        }
    }

    companion object {
        const val PLAIN_PREFIX = "mn_"
        const val KEY_BYTES = 32
        const val IDENTIFIER_LEN = 8
        private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    }
}
