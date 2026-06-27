package com.mneme.id

import java.util.UUID

/**
 * 외부 노출 ID. `<prefix>_<base32 26자>` 포맷.
 *
 * 내부 저장은 UUID v7. 외부(API/MCP/URL)에는 이 객체의 `format()` 결과만 노출한다.
 */
data class PrefixedId(
    val prefix: String,
    val uuid: UUID,
) {
    init {
        require(Prefix.ALL.contains(prefix)) { "알 수 없는 prefix: '$prefix'" }
    }

    /**
     * `<prefix>_<base32>` 형태의 외부 ID 문자열.
     */
    fun format(): String = "${prefix}_${Base32Crockford.encode(uuid)}"

    /**
     * 알려진 prefix 집합. 신규 도메인 추가 시 이 목록에도 추가.
     */
    object Prefix {
        const val MEMORY = "mem"
        const val FOLDER = "fld"
        const val TAG = "tag"
        const val API_KEY = "key"
        const val USER = "usr"
        const val OAUTH_CLIENT = "oac"
        const val OAUTH_TOKEN = "oat"
        const val SESSION = "ses"
        const val MEMORY_LINK = "lnk"
        const val MEMORY_VERSION = "ver"
        const val AUDIT_EVENT = "aud"

        val ALL = setOf(MEMORY, FOLDER, TAG, API_KEY, USER, OAUTH_CLIENT, OAUTH_TOKEN, SESSION, MEMORY_LINK, MEMORY_VERSION, AUDIT_EVENT)
    }

    companion object {
        /**
         * `<prefix>_<base32>` 문자열을 파싱한다. expectedPrefix와 일치하지 않으면 예외.
         *
         * @param extId 외부 ID 문자열 (예: `mem_2v8gqj4z7n3kh5p9r1t6w0xy2a`)
         * @param expectedPrefix 기대 prefix (예: `mem`)
         * @return PrefixedId
         * @throws IllegalArgumentException 형식 불일치 또는 prefix 불일치
         */
        fun parse(
            extId: String,
            expectedPrefix: String,
        ): PrefixedId {
            val idx = extId.indexOf('_')
            require(idx > 0) { "외부 ID에 '_' 구분자가 없습니다: '$extId'" }
            val prefix = extId.substring(0, idx)
            val body = extId.substring(idx + 1)
            require(prefix == expectedPrefix) { "prefix 불일치: 기대='$expectedPrefix', 실제='$prefix'" }
            val uuid = Base32Crockford.decode(body)
            return PrefixedId(prefix, uuid)
        }
    }
}
