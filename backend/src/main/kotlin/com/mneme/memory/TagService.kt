package com.mneme.memory

import com.mneme.id.IdFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 태그 도메인 서비스.
 *
 * 이름은 소문자로 정규화. 길이 32자 상한. 한 메모리에 최대 16개 부착.
 * `getOrCreate`는 동일 이름이 있으면 재사용, 없으면 생성.
 */
@Service
@Transactional
class TagService(
    private val tagRepository: TagRepository,
    private val memoryRepository: MemoryRepository,
    private val memoryTagRepository: MemoryTagRepository,
    private val idFactory: IdFactory,
) {
    /** 단건 또는 신규 생성. */
    fun getOrCreate(
        userId: UUID,
        rawName: String,
    ): Tag {
        val name = normalize(rawName)
        require(name.isNotBlank()) { "태그 이름은 비어 있을 수 없습니다" }
        require(name.length <= 32) { "태그 이름은 32자 이내" }
        return tagRepository.findByUserIdAndName(userId, name)
            ?: tagRepository.save(
                Tag(
                    id = idFactory.newUuid(),
                    userId = userId,
                    name = name,
                ),
            )
    }

    /** 메모리에 태그 부착. */
    fun attach(
        userId: UUID,
        memoryId: UUID,
        rawName: String,
    ): Tag {
        val memory = memoryRepository.findByUserIdAndId(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val existing = memoryTagRepository.findAllByIdMemoryId(memory.id)
        require(existing.size < MAX_TAGS_PER_MEMORY) { "메모리당 태그 최대 $MAX_TAGS_PER_MEMORY 개" }
        val tag = getOrCreate(userId, rawName)
        if (existing.none { it.id.tagId == tag.id }) {
            memoryTagRepository.save(MemoryTag(MemoryTag.MemoryTagId(memoryId = memory.id, tagId = tag.id)))
        }
        return tag
    }

    /** 메모리에서 태그 분리. */
    fun detach(
        userId: UUID,
        memoryId: UUID,
        tagId: UUID,
    ) {
        val memory = memoryRepository.findByUserIdAndId(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        memoryTagRepository.deleteById(MemoryTag.MemoryTagId(memoryId = memory.id, tagId = tagId))
    }

    /** 메모리의 태그 목록. */
    @Transactional(readOnly = true)
    fun listForMemory(
        userId: UUID,
        memoryId: UUID,
    ): List<Tag> {
        val memory = memoryRepository.findByUserIdAndId(userId, memoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val joins = memoryTagRepository.findAllByIdMemoryId(memory.id)
        val tagIds = joins.map { it.id.tagId }.toSet()
        return tagRepository.findAllByUserId(userId).filter { it.id in tagIds }
    }

    /** 사용자 본인의 모든 태그. */
    @Transactional(readOnly = true)
    fun listAll(userId: UUID): List<Tag> = tagRepository.findAllByUserId(userId)

    private fun normalize(rawName: String): String = rawName.trim().lowercase()

    companion object {
        const val MAX_TAGS_PER_MEMORY = 16
    }
}
