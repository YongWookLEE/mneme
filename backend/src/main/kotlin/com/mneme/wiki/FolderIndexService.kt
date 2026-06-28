package com.mneme.wiki

import com.mneme.llm.ChatService
import com.mneme.memory.FolderRepository
import com.mneme.memory.MemoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 폴더 인덱스 서비스.
 *
 * - [regenerate]: LLM에게 폴더 안 메모리(제목 + 요약)를 보여주고 한 페이지 마크다운을 받아 저장.
 *   외부 호출은 트랜잭션 밖에서, INSERT/UPDATE는 트랜잭션 안.
 * - [get]: 본인 폴더 인덱스 조회. 없으면 404.
 *
 * 격리: 모든 메서드 첫 인자 userId.
 *
 * @author Mneme
 * @since phase 21
 */
@Service
class FolderIndexService(
    private val folderRepository: FolderRepository,
    private val memoryRepository: MemoryRepository,
    private val folderIndexRepository: FolderIndexRepository,
    private val chatService: ChatService,
) {
    /** 본인 폴더 인덱스 조회. 없으면 NOT_FOUND. */
    @Transactional(readOnly = true)
    fun get(
        userId: UUID,
        folderId: UUID,
    ): FolderIndex =
        folderIndexRepository.findByUserIdAndFolderId(userId, folderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    /**
     * LLM 호출로 인덱스를 새로 생성하고 저장한다. 외부 호출은 트랜잭션 밖에서 수행.
     * 폴더에 메모리가 없으면 IllegalState를 던진다(컨트롤러가 400으로 매핑).
     */
    fun regenerate(
        userId: UUID,
        folderId: UUID,
    ): FolderIndex {
        val folder =
            folderRepository.findByUserIdAndId(userId, folderId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val memories = memoryRepository.findAllByUserIdAndFolderIdAndArchivedAtIsNull(userId, folderId)
        if (memories.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "folder_empty")

        val input = memories.map { ChatService.FolderIndexMemory(title = it.title, summary = it.summary) }
        val body =
            chatService.synthesizeFolderIndex(userId, folder.name, input)
                ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "llm_unavailable")
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val summary = firstLine.take(200)

        return saveIndex(userId, folderId, summary, body, memories.size)
    }

    @Transactional
    fun saveIndex(
        userId: UUID,
        folderId: UUID,
        summary: String,
        body: String,
        memoryCount: Int,
    ): FolderIndex {
        val existing = folderIndexRepository.findByUserIdAndFolderId(userId, folderId)
        if (existing != null) {
            existing.summary = summary
            existing.body = body
            existing.memoryCount = memoryCount
            existing.generatedAt = OffsetDateTime.now()
            return existing
        }
        val entity =
            FolderIndex(
                folderId = folderId,
                userId = userId,
                summary = summary,
                body = body,
                memoryCount = memoryCount,
            )
        folderIndexRepository.save(entity)
        return entity
    }
}
