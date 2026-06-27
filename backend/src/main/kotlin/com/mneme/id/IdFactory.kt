package com.mneme.id

import org.springframework.stereotype.Component
import java.util.UUID

/**
 * UUID v7 + prefix 결합 ID 팩토리.
 *
 * 도메인 서비스에서 신규 엔티티 ID를 만들 때 호출한다. UUID는 내부 저장용, format()은 외부 노출용.
 */
@Component
class IdFactory {
    /** 새 UUID v7. */
    fun newUuid(): UUID = UuidV7.newId()

    /** Memory ID(`mem_*`). */
    fun newMemoryId(): PrefixedId = PrefixedId(PrefixedId.Prefix.MEMORY, newUuid())

    /** Folder ID(`fld_*`). */
    fun newFolderId(): PrefixedId = PrefixedId(PrefixedId.Prefix.FOLDER, newUuid())

    /** Tag ID(`tag_*`). */
    fun newTagId(): PrefixedId = PrefixedId(PrefixedId.Prefix.TAG, newUuid())

    /** API Key ID(`key_*`). */
    fun newApiKeyId(): PrefixedId = PrefixedId(PrefixedId.Prefix.API_KEY, newUuid())

    /** User ID(`usr_*`). */
    fun newUserId(): PrefixedId = PrefixedId(PrefixedId.Prefix.USER, newUuid())

    /** Memory Link ID(`lnk_*`). */
    fun newMemoryLinkId(): PrefixedId = PrefixedId(PrefixedId.Prefix.MEMORY_LINK, newUuid())

    /** Audit Event ID(`aud_*`). */
    fun newAuditEventId(): PrefixedId = PrefixedId(PrefixedId.Prefix.AUDIT_EVENT, newUuid())
}
