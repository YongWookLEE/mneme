package com.mneme.export

import com.mneme.security.AuthenticatedUserResolver
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 사용자 본인의 메모리 전체를 zip으로 다운로드하는 엔드포인트.
 *
 * `GET /api/export.zip` — `application/zip` 스트림. 파일 이름은 `mneme-export-YYYY-MM-DD.zip`.
 *
 * @author Mneme
 * @since phase 13
 */
@RestController
@RequestMapping("/api/export")
class ExportController(
    private val exportService: ExportService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 전체 export. 본문은 zip stream. */
    @GetMapping(value = ["", "/zip"], produces = ["application/zip"])
    fun exportZip(response: HttpServletResponse) {
        val userId = userResolver.currentUserId()
        val filename = "mneme-export-${LocalDate.now()}.zip"
        response.contentType = "application/zip"
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
        response.setHeader("X-Content-Type-Options", "nosniff")
        exportService.exportToStream(userId, response.outputStream)
        response.flushBuffer()
    }

    @Suppress("unused")
    private val mediaTypeReference = MediaType.APPLICATION_OCTET_STREAM
}
