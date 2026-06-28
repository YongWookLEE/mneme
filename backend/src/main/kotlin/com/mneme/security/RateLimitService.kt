package com.mneme.security

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * 분/일/쓰기 token bucket. 사용자별 분리(익명 IP 별도). 모든 카운터는 in-memory(Caffeine TTL).
 *
 * 클러스터 확장 시 Redis로 교체. 현재 단일 노드 MVP 가정.
 */
@Component
class RateLimitService(
    private val props: RateLimitProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private lateinit var minuteCounter: Cache<String, AtomicInteger>
    private lateinit var dayCounter: Cache<String, AtomicInteger>
    private lateinit var writeMinuteCounter: Cache<String, AtomicInteger>

    @PostConstruct
    fun init() {
        minuteCounter =
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(100_000)
                .build()
        writeMinuteCounter =
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(100_000)
                .build()
        dayCounter =
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofDays(1))
                .maximumSize(100_000)
                .build()
    }

    /**
     * 호출 시도. true면 통과, false면 차단(429 응답 권장).
     */
    fun acquire(
        principalKey: String,
        isWrite: Boolean,
        isAuthenticated: Boolean,
    ): Boolean {
        val minLimit = if (isAuthenticated) props.perMin else props.anonymousPerMin
        val today = LocalDate.now(clock.withZone(ZoneOffset.UTC))
        val dayKey = "$principalKey:$today"

        val minNow = minuteCounter.get(principalKey) { AtomicInteger(0) }.incrementAndGet()
        if (minNow > minLimit) return false

        if (isAuthenticated) {
            val dayNow = dayCounter.get(dayKey) { AtomicInteger(0) }.incrementAndGet()
            if (dayNow > props.perDay) return false
        }

        if (isWrite) {
            val writeNow = writeMinuteCounter.get(principalKey) { AtomicInteger(0) }.incrementAndGet()
            if (writeNow > props.writePerMin) return false
        }
        return true
    }
}
