package com.mneme.memory

import com.mneme.id.IdFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 메모리 도메인 서비스(코어 CRUD).
 *
 * 본 phase에서는 LLM 분류/임베딩/요약 없이 사용자가 직접 제공한 값으로만 저장한다. 자동 분류와 임베딩은 phase 06에서
 * 트랜잭션 전후로 결합된다(외부 호출은 트랜잭션 밖, INSERT는 트랜잭션 안).
 *
 * 256KB 본문 상한, 낙관적 락(`@Version`), archive/restore(soft delete).
 */
@Service
@Transactional
class MemoryService(
    private val memoryRepository: MemoryRepository,
    private val folderRepository: FolderRepository,
    private val idFactory: IdFactory,
) {
    /** 새 메모리 작성. */
    fun create(
        userId: UUID,
        folderId: UUID,
        title: String,
        content: String,
        summary: String? = null,
        sourceUri: String? = null,
    ): Memory {
        require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다" }
        val byteSize = content.toByteArray(Charsets.UTF_8).size
        require(byteSize <= MAX_CONTENT_BYTES) { "본문은 256KB를 초과할 수 없습니다 (현재: ${byteSize}B)" }
        folderRepository.findByUserIdAndId(userId, folderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        val memory =
            Memory(
                id = idFactory.newUuid(),
                userId = userId,
                folderId = folderId,
                title = title,
                content = content,
                summary = summary,
                sourceUri = sourceUri,
                byteSize = byteSize,
            )
        memoryRepository.save(memory)
        return memory
    }

    /** 단건 조회(archived 포함). */
    @Transactional(readOnly = true)
    fun get(
        userId: UUID,
        memoryId: UUID,
    ): Memory = memoryRepository.findByUserIdAndId(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    /**
     * 본문 또는 메타 갱신. `expectedVersion`이 현재 version과 불일치하면 409.
     */
    fun update(
        userId: UUID,
        memoryId: UUID,
        expectedVersion: Long,
        title: String? = null,
        content: String? = null,
        summary: String? = null,
        folderId: UUID? = null,
    ): Memory {
        val memory =
            memoryRepository.findByUserIdAndIdAndArchivedAtIsNull(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (memory.version != expectedVersion) throw ResponseStatusException(HttpStatus.CONFLICT)

        if (title != null) {
            require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다" }
            memory.title = title
        }
        if (content != null) {
            val byteSize = content.toByteArray(Charsets.UTF_8).size
            require(byteSize <= MAX_CONTENT_BYTES) { "본문은 256KB를 초과할 수 없습니다" }
            memory.content = content
            memory.byteSize = byteSize
        }
        if (summary != null) memory.summary = summary
        if (folderId != null) {
            folderRepository.findByUserIdAndId(userId, folderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
            memory.folderId = folderId
        }
        memory.updatedAt = OffsetDateTime.now()
        return memory
    }

    /** Archive(soft delete). */
    fun archive(
        userId: UUID,
        memoryId: UUID,
    ) {
        val memory =
            memoryRepository.findByUserIdAndIdAndArchivedAtIsNull(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        memory.archivedAt = OffsetDateTime.now()
    }

    /** Restore(archived → 활성). 같은 경로에 활성 동일 제목이 있으면 409. */
    fun restore(
        userId: UUID,
        memoryId: UUID,
    ) {
        val memory = memoryRepository.findByUserIdAndId(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (memory.archivedAt == null) return
        memory.archivedAt = null
    }

    /** 사용자 본인의 활성 메모리 목록. */
    @Transactional(readOnly = true)
    fun listActive(userId: UUID): List<Memory> = memoryRepository.findAllByUserIdAndArchivedAtIsNull(userId)

    /** Archived 메모리 목록. */
    @Transactional(readOnly = true)
    fun listArchived(userId: UUID): List<Memory> = memoryRepository.findAllByUserIdAndArchivedAtIsNotNull(userId)

    companion object {
        const val MAX_CONTENT_BYTES = 262_144 // 256KB
    }
}
