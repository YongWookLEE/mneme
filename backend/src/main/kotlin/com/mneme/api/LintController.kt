package com.mneme.api

import com.mneme.security.AuthenticatedUserResolver
import com.mneme.wiki.LintService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `GET /api/lint` — 본인 메모리 lint 보고서.
 *
 * @author Mneme
 * @since phase 22
 */
@RestController
@RequestMapping("/api/lint")
class LintController(
    private val lintService: LintService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 본인 lint 결과 단건. */
    @GetMapping
    fun lint(): LintService.LintReport {
        val userId = userResolver.currentUserId()
        return lintService.runAll(userId)
    }
}
