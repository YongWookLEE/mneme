package com.mneme.id

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * UUID v7 생성기 단위 테스트.
 */
class UuidV7Test {
    /**
     * version 비트가 7인지 확인. UUID v7은 msb의 16~19 비트가 0111.
     */
    @Test
    fun `생성된 UUID 의 version 은 7 이다`() {
        repeat(50) {
            val id = UuidV7.newId()
            id.version() shouldBe 7
        }
    }

    /**
     * variant 비트가 RFC 4122(10)인지 확인.
     */
    @Test
    fun `variant 는 RFC 4122 형식이다`() {
        repeat(50) {
            val id = UuidV7.newId()
            id.variant() shouldBe 2
        }
    }

    /**
     * 같은 ms 내 1000개 생성해도 단조 증가.
     */
    @Test
    fun `같은 ms 내 단조 증가가 보장된다`() {
        val ids = (1..1000).map { UuidV7.newId() }
        for (i in 1 until ids.size) {
            (ids[i] > ids[i - 1]) shouldBe true
        }
    }

    /**
     * 4 스레드 × 500 생성 시 중복 없음 + 모두 v7.
     */
    @Test
    fun `멀티스레드 환경에서 중복이 없다`() {
        val set = ConcurrentSkipListSet<String>()
        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(4)
        repeat(4) {
            executor.submit {
                try {
                    repeat(500) {
                        set.add(UuidV7.newId().toString())
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, TimeUnit.SECONDS) shouldBe true
        executor.shutdown()
        set.size shouldBe 2000
    }

    @Test
    fun `생성된 UUID 는 null 이 아니다`() {
        UuidV7.newId() shouldNotBe null
    }
}
