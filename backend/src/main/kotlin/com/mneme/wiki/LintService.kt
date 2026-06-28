package com.mneme.wiki

import com.mneme.id.PrefixedId
import com.mneme.memory.MemoryLinkRepository
import com.mneme.memory.MemoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 메모리 lint — 본인 메모리에서 약점을 감지한다.
 *
 * 규칙(MVP):
 * - **broken**: 본문에 `[[…]]`이 있지만 타겟 메모리가 없는 경우 (target_id=null).
 * - **orphan**: 다른 메모리에서 참조하지도 않고 자기 본문에서 참조하지도 않는 외톨이.
 * - **stub**: 본문이 매우 짧음(120B 미만)인 메모리(미완성 추측).
 * - **dup-title**: 같은 폴더 안에서 동일 제목이 여러 번 등장(소문자 비교).
 *
 * 격리: 모든 메서드 첫 인자 userId.
 *
 * @author Mneme
 * @since phase 22
 */
@Service
class LintService(
    private val memoryRepository: MemoryRepository,
    private val memoryLinkRepository: MemoryLinkRepository,
) {
    /** 본인 활성 메모리에 대한 lint 결과. 카테고리별로 묶어 반환. */
    @Transactional(readOnly = true)
    fun runAll(userId: UUID): LintReport {
        val memories = memoryRepository.findAllByUserIdAndArchivedAtIsNull(userId)
        val memoryById = memories.associateBy { it.id }
        val linkedSourceIds = mutableSetOf<UUID>()
        val linkedTargetIds = mutableSetOf<UUID>()
        val broken = mutableListOf<LintIssue>()

        for (m in memories) {
            val links = memoryLinkRepository.findAllByUserIdAndSourceId(userId, m.id)
            for (link in links) {
                linkedSourceIds.add(m.id)
                if (link.targetId != null && link.targetId in memoryById) {
                    linkedTargetIds.add(link.targetId!!)
                } else {
                    broken.add(
                        LintIssue(
                            kind = "broken",
                            memoryExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                            memoryTitle = m.title,
                            detail = "[[${link.targetLabel}]] 대응 메모리 없음",
                        ),
                    )
                }
            }
        }

        val orphans =
            memories.filter { it.id !in linkedSourceIds && it.id !in linkedTargetIds }.map { m ->
                LintIssue(
                    kind = "orphan",
                    memoryExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                    memoryTitle = m.title,
                    detail = "어떤 메모리와도 연결되지 않음",
                )
            }

        val stubs =
            memories.filter { it.byteSize < STUB_THRESHOLD_BYTES }.map { m ->
                LintIssue(
                    kind = "stub",
                    memoryExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                    memoryTitle = m.title,
                    detail = "본문이 ${m.byteSize}B로 짧음(미완성 추정)",
                )
            }

        val dupTitles =
            memories
                .groupBy { (it.folderId to it.title.trim().lowercase()) }
                .filter { it.value.size > 1 }
                .flatMap { (_, group) ->
                    group.map { m ->
                        LintIssue(
                            kind = "dup-title",
                            memoryExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                            memoryTitle = m.title,
                            detail = "같은 폴더의 다른 메모리와 제목 중복",
                        )
                    }
                }

        return LintReport(
            counts =
                mapOf(
                    "broken" to broken.size,
                    "orphan" to orphans.size,
                    "stub" to stubs.size,
                    "dup-title" to dupTitles.size,
                ),
            issues = broken + orphans + stubs + dupTitles,
        )
    }

    /** 한 건의 lint 이슈. */
    data class LintIssue(
        val kind: String,
        val memoryExtId: String,
        val memoryTitle: String,
        val detail: String,
    )

    /** lint 보고서. */
    data class LintReport(
        val counts: Map<String, Int>,
        val issues: List<LintIssue>,
    )

    companion object {
        const val STUB_THRESHOLD_BYTES = 120
    }
}
