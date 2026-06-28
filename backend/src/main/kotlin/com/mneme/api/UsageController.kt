package com.mneme.api

import com.mneme.observability.UsageDailyRepository
import com.mneme.observability.UsageDailyRow
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 본인 일별 사용량 — `GET /api/usage?from=YYYY-MM-DD&to=YYYY-MM-DD`.
 *
 * 미지정 시 최근 30일. 응답은 일자 desc.
 *
 * @author Mneme
 * @since phase 14
 */
@RestController
@RequestMapping("/api/usage")
class UsageController(
    private val usageRepository: UsageDailyRepository,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 사용량 조회. */
    @GetMapping
    fun usage(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
    ): List<UsageDailyRow> {
        val userId = userResolver.currentUserId()
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val fromDate = from?.let { LocalDate.parse(it) } ?: toDate.minusDays(30)
        return usageRepository.findRange(userId, fromDate, toDate)
    }
}
