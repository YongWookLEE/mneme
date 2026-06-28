package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.security.AuthenticatedUserResolver
import com.mneme.wiki.FolderIndexService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * 폴더 인덱스 REST. `GET /api/folders/{ext}/index` 조회, `POST /api/folders/{ext}/index/rebuild` 재생성.
 *
 * @author Mneme
 * @since phase 21
 */
@RestController
@RequestMapping("/api/folders")
class FolderIndexController(
    private val service: FolderIndexService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 본인 폴더 인덱스 조회. 없으면 404. */
    @GetMapping("/{extId}/index")
    fun get(
        @PathVariable extId: String,
    ): IndexResponse {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(extId, PrefixedId.Prefix.FOLDER).uuid
        val idx = service.get(userId, folderId)
        return IndexResponse(
            folderExtId = PrefixedId(PrefixedId.Prefix.FOLDER, idx.folderId).format(),
            summary = idx.summary,
            body = idx.body,
            memoryCount = idx.memoryCount,
            generatedAt = idx.generatedAt,
        )
    }

    /** LLM 호출로 인덱스 새로 생성. 사용자가 명시적으로 트리거. */
    @PostMapping("/{extId}/index/rebuild")
    fun rebuild(
        @PathVariable extId: String,
    ): IndexResponse {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(extId, PrefixedId.Prefix.FOLDER).uuid
        val idx = service.regenerate(userId, folderId)
        return IndexResponse(
            folderExtId = PrefixedId(PrefixedId.Prefix.FOLDER, idx.folderId).format(),
            summary = idx.summary,
            body = idx.body,
            memoryCount = idx.memoryCount,
            generatedAt = idx.generatedAt,
        )
    }

    /** 인덱스 응답 DTO. */
    data class IndexResponse(
        val folderExtId: String,
        val summary: String,
        val body: String,
        val memoryCount: Int,
        val generatedAt: OffsetDateTime,
    )
}
