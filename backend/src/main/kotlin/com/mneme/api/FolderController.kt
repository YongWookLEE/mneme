package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.memory.Folder
import com.mneme.memory.FolderService
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * 폴더 관리 REST 컨트롤러.
 *
 * 외부 ID 형식 `fld_<base32>`. 내부 UUID는 응답에 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/folders")
class FolderController(
    private val folderService: FolderService,
    private val userResolver: AuthenticatedUserResolver,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody body: CreateFolderRequest,
    ): FolderResponse {
        val userId = userResolver.currentUserId()
        val parentId = body.parentExtId?.let { PrefixedId.parse(it, PrefixedId.Prefix.FOLDER).uuid }
        val folder = folderService.create(userId, parentId, body.name)
        return toResponse(folder)
    }

    @GetMapping
    fun listAll(): List<FolderResponse> {
        val userId = userResolver.currentUserId()
        return folderService.listAll(userId).map { toResponse(it) }
    }

    @GetMapping("/{extId}")
    fun get(
        @PathVariable extId: String,
    ): FolderResponse {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(extId, PrefixedId.Prefix.FOLDER).uuid
        return toResponse(folderService.get(userId, folderId))
    }

    @PatchMapping("/{extId}")
    fun rename(
        @PathVariable extId: String,
        @RequestBody body: RenameFolderRequest,
    ): FolderResponse {
        val userId = userResolver.currentUserId()
        val folderId = PrefixedId.parse(extId, PrefixedId.Prefix.FOLDER).uuid
        return toResponse(folderService.rename(userId, folderId, body.name))
    }

    private fun toResponse(folder: Folder): FolderResponse =
        FolderResponse(
            extId = PrefixedId(PrefixedId.Prefix.FOLDER, folder.id).format(),
            parentExtId = folder.parentId?.let { PrefixedId(PrefixedId.Prefix.FOLDER, it).format() },
            path = folder.path,
            name = folder.name,
            createdAt = folder.createdAt,
        )

    data class CreateFolderRequest(
        val parentExtId: String?,
        val name: String,
    )

    data class RenameFolderRequest(
        val name: String,
    )

    data class FolderResponse(
        val extId: String,
        val parentExtId: String?,
        val path: String,
        val name: String,
        val createdAt: OffsetDateTime,
    )
}
