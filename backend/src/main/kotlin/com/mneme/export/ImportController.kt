package com.mneme.export

import com.mneme.security.AuthenticatedUserResolver
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 메모리 import 엔드포인트.
 *
 * - `POST /api/import/preview` (multipart `file`) — zip을 파싱해 항목 목록 + 충돌 표시 반환.
 * - `POST /api/import/apply` (JSON body) — preview에서 받은 sessionId + 사용자 결정으로 실제 import 수행.
 *
 * @author Mneme
 * @since phase 13
 */
@RestController
@RequestMapping("/api/import")
class ImportController(
    private val importService: ImportService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** zip 미리보기. multipart 파일 받음. */
    @PostMapping("/preview")
    fun preview(
        @RequestPart("file") file: MultipartFile,
    ): ImportService.PreviewResult {
        val userId = userResolver.currentUserId()
        return importService.preview(userId, file)
    }

    /** 사용자 결정에 따라 실제 import 수행. */
    @PostMapping("/apply")
    fun apply(
        @RequestParam("sessionId") sessionId: String,
        @RequestBody decisions: List<ImportService.ApplyDecision>,
    ): ImportService.ApplyResult {
        val userId = userResolver.currentUserId()
        return importService.apply(userId, sessionId, decisions)
    }
}
