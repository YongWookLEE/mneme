package com.mneme.wiki

/**
 * 본문 안 `[[wiki-link]]` 파싱.
 *
 * 지원 형태:
 * - `[[메모리 제목]]` — 제목 매칭
 * - `[[mem_<base32>]]` — 외부 ID 직접
 *
 * 코드 블록(``` ... ``` 또는 ` ... `) 안의 텍스트는 무시한다.
 *
 * @author Mneme
 * @since phase 16
 */
object WikiLinkParser {
    private val LINK_REGEX = Regex("""\[\[([^\[\]\n]+?)]]""")

    /** 본문에서 모든 `[[link]]` 텍스트와 시작 오프셋을 찾는다. */
    fun parse(content: String): List<ParsedLink> {
        val masked = maskCode(content)
        return LINK_REGEX
            .findAll(masked)
            .map { match ->
                val raw = match.groupValues[1].trim()
                val isExtId = raw.startsWith("mem_")
                ParsedLink(
                    offset = match.range.first,
                    rawText = raw,
                    memoryExtId = if (isExtId) raw else null,
                    titleHint = if (isExtId) null else raw,
                )
            }.toList()
    }

    /** 본문 안 코드 블록을 같은 길이 공백으로 마스킹해 정규식이 잡지 않도록 한다. */
    private fun maskCode(text: String): String {
        val sb = StringBuilder(text)
        // 펜스 코드 블록 ``` ... ```
        val fence = Regex("""```[\s\S]*?```""")
        for (m in fence.findAll(text)) {
            for (i in m.range) if (sb[i] != '\n') sb.setCharAt(i, ' ')
        }
        // 인라인 코드 ` ... ` — 줄바꿈은 포함하지 않음
        val inline = Regex("""`[^`\n]+?`""")
        for (m in inline.findAll(sb.toString())) {
            for (i in m.range) sb.setCharAt(i, ' ')
        }
        return sb.toString()
    }

    /** 파싱된 1건의 링크. */
    data class ParsedLink(
        val offset: Int,
        val rawText: String,
        val memoryExtId: String?,
        val titleHint: String?,
    )
}
