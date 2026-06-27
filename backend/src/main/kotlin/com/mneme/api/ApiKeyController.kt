package com.mneme.api

import com.mneme.auth.ApiKey
import com.mneme.auth.ApiKeyService
import com.mneme.auth.UserRepository
import com.mneme.id.PrefixedId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/**
 * API 키 관리 REST 컨트롤러.
 *
 * 세션 인증 사용자(OAuth2User)만 접근 가능. 발급 응답(`POST`)만 평문 키를 1회 포함하고,
 * 이후 조회 응답은 절대 평문을 포함하지 않는다.
 *
 * 외부 ID는 `key_<base32>` 포맷. 내부 UUID 노출 금지.
 */
@RestController
@RequestMapping("/api/keys")
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
    private val userRepository: UserRepository,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(
        @AuthenticationPrincipal principal: OAuth2User,
        @RequestBody body: IssueRequest,
    ): IssueResponse {
        val userId = userIdFromPrincipal(principal)
        val issued = apiKeyService.issue(userId, body.name)
        return IssueResponse(
            extId = externalKeyId(issued.key.id),
            name = issued.key.name,
            plaintext = issued.plaintext,
            prefix = issued.key.prefix,
            createdAt = issued.key.createdAt,
        )
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: OAuth2User,
    ): List<KeyResponse> {
        val userId = userIdFromPrincipal(principal)
        return apiKeyService.listActive(userId).map { toResponse(it) }
    }

    @PatchMapping("/{extId}")
    fun rename(
        @AuthenticationPrincipal principal: OAuth2User,
        @PathVariable extId: String,
        @RequestBody body: RenameRequest,
    ): KeyResponse {
        val userId = userIdFromPrincipal(principal)
        val keyId = parseKeyId(extId)
        val ok = apiKeyService.rename(userId, keyId, body.name)
        if (!ok) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val updated =
            apiKeyService.listActive(userId).firstOrNull { it.id == keyId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return toResponse(updated)
    }

    @DeleteMapping("/{extId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @AuthenticationPrincipal principal: OAuth2User,
        @PathVariable extId: String,
    ) {
        val userId = userIdFromPrincipal(principal)
        val keyId = parseKeyId(extId)
        val ok = apiKeyService.revoke(userId, keyId)
        if (!ok) throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/{extId}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    fun rotate(
        @AuthenticationPrincipal principal: OAuth2User,
        @PathVariable extId: String,
    ): IssueResponse {
        val userId = userIdFromPrincipal(principal)
        val keyId = parseKeyId(extId)
        val issued = apiKeyService.rotate(userId, keyId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return IssueResponse(
            extId = externalKeyId(issued.key.id),
            name = issued.key.name,
            plaintext = issued.plaintext,
            prefix = issued.key.prefix,
            createdAt = issued.key.createdAt,
        )
    }

    private fun userIdFromPrincipal(principal: OAuth2User): UUID {
        val sub = principal.getAttribute<String>("sub") ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return userRepository.findByGoogleSub(sub)?.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
    }

    private fun externalKeyId(id: UUID): String = PrefixedId(PrefixedId.Prefix.API_KEY, id).format()

    private fun parseKeyId(extId: String): UUID = PrefixedId.parse(extId, PrefixedId.Prefix.API_KEY).uuid

    private fun toResponse(key: ApiKey): KeyResponse =
        KeyResponse(
            extId = externalKeyId(key.id),
            name = key.name,
            prefix = key.prefix,
            lastUsedAt = key.lastUsedAt,
            createdAt = key.createdAt,
        )

    /** 발급 요청. */
    data class IssueRequest(
        val name: String,
    )

    /** 이름 수정 요청. */
    data class RenameRequest(
        val name: String,
    )

    /** 발급 응답 — 평문 1회 노출. */
    data class IssueResponse(
        val extId: String,
        val name: String,
        val plaintext: String,
        val prefix: String,
        val createdAt: OffsetDateTime,
    )

    /** 조회 응답 — 평문 미포함. */
    data class KeyResponse(
        val extId: String,
        val name: String,
        val prefix: String,
        val lastUsedAt: OffsetDateTime?,
        val createdAt: OffsetDateTime,
    )
}
