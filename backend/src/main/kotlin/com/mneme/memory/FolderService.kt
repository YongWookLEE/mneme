package com.mneme.memory

import com.mneme.id.IdFactory
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 폴더 도메인 서비스.
 *
 * materialized path(`path` 컬럼)는 루트 `/`로 시작해 슬래시로 종료된다(`/projects/mneme/`).
 * 이동(`move`)은 자식 폴더와 메모리의 path를 일괄 갱신해야 하나 본 step에서는 폴더만 처리한다(메모리 path 일괄 갱신은 phase 07 이후 검색·필터 도입 시).
 *
 * 모든 메서드 첫 인자 `userId: UUID` — 격리 강제.
 */
@Service
@Transactional
class FolderService(
    private val folderRepository: FolderRepository,
    private val idFactory: IdFactory,
) {
    private val log = LoggerFactory.getLogger(FolderService::class.java)

    /** 새 폴더 생성. parentId=null이면 루트 직속. */
    fun create(
        userId: UUID,
        parentId: UUID?,
        name: String,
    ): Folder {
        require(name.isNotBlank()) { "폴더 이름은 비어 있을 수 없습니다" }
        require(!name.contains('/')) { "폴더 이름에 '/'를 포함할 수 없습니다" }
        val parent = parentId?.let { folderRepository.findByUserIdAndId(userId, it) }
        if (parentId != null && parent == null) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val path = (parent?.path ?: "/") + "$name/"
        if (folderRepository.findByUserIdAndPath(userId, path) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        }
        val folder =
            Folder(
                id = idFactory.newUuid(),
                userId = userId,
                parentId = parentId,
                path = path,
                name = name,
            )
        folderRepository.save(folder)
        log.debug("Folder created: userId={}, path={}", userId, path)
        return folder
    }

    /** 폴더 이름 수정 — path도 함께 변경(자식 path 갱신은 본 step 범위 외). */
    fun rename(
        userId: UUID,
        folderId: UUID,
        newName: String,
    ): Folder {
        require(newName.isNotBlank() && !newName.contains('/')) { "이름 형식이 잘못됐습니다" }
        val folder = folderRepository.findByUserIdAndId(userId, folderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val parentPath = folder.path.dropLast(1).substringBeforeLast('/') + "/"
        val newPath = parentPath + "$newName/"
        if (folderRepository.findByUserIdAndPath(userId, newPath) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        }
        folder.name = newName
        folder.path = newPath
        return folder
    }

    /** 단건 조회. 다른 사용자의 폴더는 404. */
    @Transactional(readOnly = true)
    fun get(
        userId: UUID,
        folderId: UUID,
    ): Folder = folderRepository.findByUserIdAndId(userId, folderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    /** 사용자 본인의 모든 폴더. */
    @Transactional(readOnly = true)
    fun listAll(userId: UUID): List<Folder> = folderRepository.findAllByUserId(userId)
}
