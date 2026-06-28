package com.mneme.wiki

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 사용자 최근 피드백을 LLM 시스템 프롬프트에 덧붙일 짧은 문자열로 변환한다.
 *
 * 너무 길어지면 토큰 낭비이므로 최근 N건 + 부정 피드백 우선 + 한 줄당 1건으로 잘라 노출한다.
 *
 * 결과 예:
 *   사용자 피드백 (최근):
 *   - down folder: 분류가 너무 일반적이었음
 *   - up summary
 *   - down tags: 영문 태그보다 한글을 선호
 *
 * 결과가 비어 있으면 빈 문자열을 반환한다(호출 측에서 빈 문자열은 시스템 프롬프트에 붙이지 않음).
 *
 * @author Mneme
 * @since phase 23
 */
@Component
class FeedbackHintBuilder(
    private val repository: MemoryFeedbackRepository,
) {
    /** 사용자 최근 피드백 요약. 비어 있으면 빈 문자열. */
    @Transactional(readOnly = true)
    fun buildFor(userId: UUID): String {
        val all = repository.findAllByUserIdOrderByCreatedAtDesc(userId)
        if (all.isEmpty()) return ""
        // 부정 피드백을 앞쪽으로 (학습할 거리), 최근 + 최대 8개.
        val sorted = all.sortedWith(compareByDescending<MemoryFeedback> { it.value == "down" }.thenByDescending { it.createdAt })
        val picked = sorted.take(MAX_LINES)
        val lines =
            picked.joinToString("\n") { f ->
                val note = f.note?.let { ": $it" } ?: ""
                "- ${f.value} ${f.target}$note"
            }
        return "\n\n사용자 피드백 (최근):\n$lines\n위 신호를 응답에 반영하라."
    }

    companion object {
        const val MAX_LINES = 8
    }
}
