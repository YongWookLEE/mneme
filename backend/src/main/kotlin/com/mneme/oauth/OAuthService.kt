package com.mneme.oauth

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mneme.id.IdFactory
import jakarta.annotation.PostConstruct
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

/**
 * OAuth 클라이언트 등록·인가 코드·토큰 발급 서비스.
 *
 * DCR(RFC 7591) — `/oauth/register`에서 client_id/client_secret 발급. client_secret는 sha256만 저장.
 * Authorization Code 흐름 — code는 Caffeine 10분 TTL 인메모리. 단일 사용 후 폐기.
 * Token — access(30분) + refresh(30일). 둘 다 sha256만 저장.
 *
 * PKCE(S256)는 보안상 필수. code_verifier가 일치하지 않으면 token 거부.
 *
 * @author Mneme
 * @since phase 10
 */
@Service
@Transactional
class OAuthService(
    private val clientRepo: OAuthClientRepository,
    private val tokenRepo: OAuthTokenRepository,
    private val props: OAuthProperties,
    private val idFactory: IdFactory,
) {
    private val random = SecureRandom()
    private lateinit var codeCache: Cache<String, AuthorizationCode>

    @PostConstruct
    fun init() {
        codeCache =
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofSeconds(props.codeTtlSeconds))
                .maximumSize(10_000)
                .build()
    }

    /** DCR 등록. */
    fun registerClient(
        redirectUris: List<String>,
        clientName: String?,
    ): RegisteredClient {
        require(redirectUris.isNotEmpty()) { "redirect_uris는 비어 있을 수 없습니다" }
        require(redirectUris.all { it.startsWith("https://") || it.startsWith("http://localhost") || it.startsWith("http://127.0.0.1") }) {
            "redirect_uri는 https 또는 localhost만 허용됩니다"
        }
        val clientId = "oac_" + randomToken(18)
        val secret = randomToken(32)
        val client =
            OAuthClient(
                id = idFactory.newUuid(),
                userId = null,
                clientId = clientId,
                clientSecretHash = sha256(secret),
                redirectUris = redirectUris.toTypedArray(),
                clientName = clientName,
            )
        clientRepo.save(client)
        return RegisteredClient(clientId = clientId, clientSecret = secret, redirectUris = redirectUris, clientName = clientName)
    }

    /** 인가 코드 발급. authorize 컨트롤러가 user 동의 완료 후 호출. */
    fun issueCode(
        userId: UUID,
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        codeChallengeMethod: String,
    ): String {
        val client = clientRepo.findByClientId(clientId) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_client")
        if (!client.redirectUris.contains(redirectUri)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri_mismatch")
        }
        if (codeChallengeMethod != "S256") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "code_challenge_method must be S256")
        }
        require(codeChallenge.isNotBlank()) { "code_challenge required" }
        val code = randomToken(24)
        codeCache.put(
            code,
            AuthorizationCode(
                userId = userId,
                clientId = clientId,
                redirectUri = redirectUri,
                codeChallenge = codeChallenge,
            ),
        )
        return code
    }

    /** code → 토큰 교환. */
    fun exchangeCode(
        code: String,
        clientId: String,
        clientSecret: String?,
        redirectUri: String,
        codeVerifier: String,
    ): IssuedTokens {
        val client = clientRepo.findByClientId(clientId) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client")
        if (client.clientSecretHash != null) {
            if (clientSecret == null || !MessageDigest.isEqual(sha256(clientSecret), client.clientSecretHash)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client")
            }
        }
        val payload = codeCache.getIfPresent(code) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_grant")
        codeCache.invalidate(code) // 1회 사용
        if (payload.clientId != clientId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "client_mismatch")
        if (payload.redirectUri != redirectUri) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri_mismatch")
        val expectedChallenge = pkceS256(codeVerifier)
        if (expectedChallenge != payload.codeChallenge) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "code_verifier_invalid")

        return mintTokens(payload.userId, clientId)
    }

    /** refresh token → 새 access/refresh 발급(회전). */
    fun refresh(
        refreshToken: String,
        clientId: String,
        clientSecret: String?,
    ): IssuedTokens {
        val client = clientRepo.findByClientId(clientId) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client")
        if (client.clientSecretHash != null) {
            if (clientSecret == null || !MessageDigest.isEqual(sha256(clientSecret), client.clientSecretHash)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client")
            }
        }
        val existing =
            tokenRepo.findByRefreshHashAndRevokedAtIsNull(sha256(refreshToken))
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_grant")
        if (existing.clientId != clientId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "client_mismatch")
        if (existing.refreshExpiresAt != null && existing.refreshExpiresAt!!.isBefore(OffsetDateTime.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_expired")
        }
        existing.revokedAt = OffsetDateTime.now()
        return mintTokens(existing.userId, clientId)
    }

    /** 토큰 인증 — Bearer 헤더 처리용. */
    @Transactional(readOnly = true)
    fun authenticateAccessToken(accessToken: String): UUID? {
        val hash = sha256(accessToken)
        val token = tokenRepo.findByAccessHashAndRevokedAtIsNull(hash) ?: return null
        if (token.expiresAt.isBefore(OffsetDateTime.now())) return null
        return token.userId
    }

    private fun mintTokens(
        userId: UUID,
        clientId: String,
    ): IssuedTokens {
        val access = randomToken(32)
        val refresh = randomToken(32)
        val now = OffsetDateTime.now()
        val accessExpiry = now.plusMinutes(props.accessTtlMinutes)
        val refreshExpiry = now.plusDays(props.refreshTtlDays)
        val token =
            OAuthToken(
                id = idFactory.newUuid(),
                userId = userId,
                clientId = clientId,
                accessHash = sha256(access),
                refreshHash = sha256(refresh),
                scope = "mcp",
                expiresAt = accessExpiry,
                refreshExpiresAt = refreshExpiry,
            )
        tokenRepo.save(token)
        return IssuedTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresInSeconds = props.accessTtlMinutes * 60,
        )
    }

    private fun randomToken(bytes: Int): String {
        val buf = ByteArray(bytes)
        random.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun sha256(text: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))

    private fun pkceS256(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /** 인가 코드 페이로드(Caffeine 값). */
    data class AuthorizationCode(
        val userId: UUID,
        val clientId: String,
        val redirectUri: String,
        val codeChallenge: String,
    )

    /** DCR 등록 결과(평문 client_secret 1회 노출). */
    data class RegisteredClient(
        val clientId: String,
        val clientSecret: String,
        val redirectUris: List<String>,
        val clientName: String?,
    )

    /** Token 발급 결과(평문 1회 노출). */
    data class IssuedTokens(
        val accessToken: String,
        val refreshToken: String,
        val expiresInSeconds: Long,
    )
}
