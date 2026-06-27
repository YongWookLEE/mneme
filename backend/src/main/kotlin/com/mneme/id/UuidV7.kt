package com.mneme.id

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * UUID v7 생성기.
 *
 * 비트 레이아웃(128비트):
 *   [unix_ms_48 | version_4 | rand_a_12 | variant_2 | rand_b_62]
 *
 * `rand_a`는 동일 밀리초 내 단조 증가를 보장하기 위한 카운터 + 랜덤 시드 조합으로 사용한다.
 * 같은 ms에 여러 개가 생성되어도 결과 UUID는 엄격하게 ascending. 카운터가 12비트(4096)를 넘기면
 * 다음 ms로 넘어가도록 wait한다(밀리초당 4096개 이상 생성은 비현실적이지만 가드).
 */
object UuidV7 {
    private val random = SecureRandom()
    private val lastMs = AtomicLong(0L)
    private val counter = AtomicLong(0L)
    private val mutex = Any()

    /**
     * 새 UUID v7을 생성한다. 스레드 안전.
     *
     * @return UUID v7 인스턴스
     */
    fun newId(): UUID {
        synchronized(mutex) {
            var now = System.currentTimeMillis()
            val last = lastMs.get()
            if (now > last) {
                lastMs.set(now)
                counter.set(0L)
            } else {
                // 같은 ms 또는 시계 역행 — counter 증가
                val c = counter.incrementAndGet()
                if (c >= 0x1000) {
                    // 카운터 포화 — 다음 ms 대기
                    while (System.currentTimeMillis() <= last) {
                        // busy wait, 일반적으론 1ms 미만
                    }
                    now = System.currentTimeMillis()
                    lastMs.set(now)
                    counter.set(0L)
                }
            }

            val ms = now
            val randA = counter.get() and 0xFFFL
            val randB = randomLong62()

            // 상위 64비트: ms(48) | version(4) | randA(12)
            val msb = (ms and 0xFFFFFFFFFFFFL) shl 16 or (0x7L shl 12) or randA
            // 하위 64비트: variant(2) | randB(62)
            val lsb = (0b10L shl 62) or randB

            return UUID(msb, lsb)
        }
    }

    /**
     * variant 2비트를 제외한 62비트 랜덤.
     */
    private fun randomLong62(): Long {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        var v = 0L
        for (b in bytes) {
            v = (v shl 8) or (b.toLong() and 0xFFL)
        }
        return v and 0x3FFFFFFFFFFFFFFFL
    }
}
