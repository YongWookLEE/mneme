package com.mneme.wiki

import org.junit.jupiter.api.Test

/**
 * WikiLinkParser 단위 테스트.
 *
 * 케이스:
 * - 일반 본문 안 [[제목]] / [[mem_…]]
 * - 코드 블록 내부 무시
 * - 인라인 코드 내부 무시
 * - 줄바꿈 포함 시 정확한 오프셋
 *
 * @author Mneme
 * @since phase 16
 */
class WikiLinkParserTest {
    @Test
    fun `제목 링크와 ext id 링크 분리`() {
        val text = "보세요 [[코틀린 메모]] 그리고 [[mem_01abc]] 둘 다."
        val links = WikiLinkParser.parse(text)
        check(links.size == 2) { "expected 2, got ${links.size}" }
        check(links[0].titleHint == "코틀린 메모")
        check(links[0].memoryExtId == null)
        check(links[1].memoryExtId == "mem_01abc")
        check(links[1].titleHint == null)
    }

    @Test
    fun `펜스 코드 블록 안의 링크는 무시`() {
        val text = "본문 [[보임]] 다음\n```\n예시 [[숨김]] 코드\n```\n다시 [[보임2]]."
        val links = WikiLinkParser.parse(text).map { it.titleHint }
        check(links == listOf("보임", "보임2")) { "actual=$links" }
    }

    @Test
    fun `인라인 코드 안의 링크는 무시`() {
        val text = "이건 인라인 `[[숨김]]` 무시. [[보임]] 만 남음."
        val links = WikiLinkParser.parse(text).map { it.titleHint }
        check(links == listOf("보임")) { "actual=$links" }
    }

    @Test
    fun `빈 본문은 빈 결과`() {
        check(WikiLinkParser.parse("").isEmpty())
        check(WikiLinkParser.parse("그냥 텍스트").isEmpty())
    }
}
