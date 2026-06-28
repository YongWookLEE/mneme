package com.mneme.oauth

import com.mneme.id.IdFactory
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

/**
 * OAuth 서비스 단위 테스트(mock 기반).
 *
 * DCR → authorize → token 코어 흐름 + PKCE S256 검증 + refresh 회전 + 격리(다른 client_id로는 토큰 발급 불가).
 *
 * @author Mneme
 * @since phase 10
 */
class OAuthServiceTest {
    private val idFactory = IdFactory()
    private val props = OAuthProperties()

    private fun freshService(): Triple<OAuthService, OAuthClientRepository, OAuthTokenRepository> {
        val clientRepo = mockk<OAuthClientRepository>(relaxed = true)
        val tokenRepo = mockk<OAuthTokenRepository>(relaxed = true)
        val saveSlot = slot<OAuthClient>()
        every { clientRepo.save(capture(saveSlot)) } answers { saveSlot.captured }
        val service = OAuthService(clientRepo, tokenRepo, props, idFactory)
        service.init()
        return Triple(service, clientRepo, tokenRepo)
    }

    @Test
    fun `DCR 등록 후 client_id-client_secret이 발급됨`() {
        val (service, _, _) = freshService()
        val registered = service.registerClient(listOf("https://example.com/cb"), "Claude.ai")
        check(registered.clientId.startsWith("oac_")) { "clientId prefix mismatch: ${registered.clientId}" }
        check(registered.clientSecret.isNotEmpty())
    }

    @Test
    fun `localhost가 아닌 http redirect_uri는 거부`() {
        val (service, _, _) = freshService()
        shouldThrow<IllegalArgumentException> {
            service.registerClient(listOf("http://example.com/cb"), null)
        }
    }

    @Test
    fun `authorize-token 정상 흐름 + PKCE S256 검증`() {
        val (service, clientRepo, tokenRepo) = freshService()
        val client =
            OAuthClient(
                id = idFactory.newUuid(),
                userId = null,
                clientId = "oac_test",
                clientSecretHash = null,
                redirectUris = arrayOf("https://example.com/cb"),
                clientName = "t",
            )
        every { clientRepo.findByClientId("oac_test") } returns client

        val verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-12345"
        val challenge =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

        val userId = idFactory.newUuid()
        val code = service.issueCode(userId, "oac_test", "https://example.com/cb", challenge, "S256")
        check(code.isNotEmpty())

        val capturedToken = slot<OAuthToken>()
        every { tokenRepo.save(capture(capturedToken)) } answers { capturedToken.captured }

        val tokens = service.exchangeCode(code, "oac_test", null, "https://example.com/cb", verifier)
        check(tokens.accessToken.isNotEmpty())
        check(tokens.refreshToken.isNotEmpty())
        check(capturedToken.captured.userId == userId)
    }

    @Test
    fun `잘못된 code_verifier 는 BAD_REQUEST`() {
        val (service, clientRepo, _) = freshService()
        val client =
            OAuthClient(
                id = idFactory.newUuid(),
                userId = null,
                clientId = "oac_t",
                clientSecretHash = null,
                redirectUris = arrayOf("https://e/cb"),
                clientName = null,
            )
        every { clientRepo.findByClientId("oac_t") } returns client
        val verifier = "good-verifier-1234567890"
        val challenge =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val code = service.issueCode(idFactory.newUuid(), "oac_t", "https://e/cb", challenge, "S256")
        shouldThrow<ResponseStatusException> {
            service.exchangeCode(code, "oac_t", null, "https://e/cb", "wrong-verifier")
        }
    }

    @Test
    fun `다른 client_id 의 code 재사용은 거부`() {
        val (service, clientRepo, tokenRepo) = freshService()
        val clientA =
            OAuthClient(
                id = idFactory.newUuid(),
                userId = null,
                clientId = "oac_A",
                clientSecretHash = null,
                redirectUris = arrayOf("https://a/cb"),
                clientName = null,
            )
        val clientB =
            OAuthClient(
                id = idFactory.newUuid(),
                userId = null,
                clientId = "oac_B",
                clientSecretHash = null,
                redirectUris = arrayOf("https://b/cb"),
                clientName = null,
            )
        every { clientRepo.findByClientId("oac_A") } returns clientA
        every { clientRepo.findByClientId("oac_B") } returns clientB
        every { tokenRepo.save(any()) } answers { it.invocation.args[0] as OAuthToken }

        val verifier = "abc-1234567890-xyz"
        val challenge =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val code = service.issueCode(idFactory.newUuid(), "oac_A", "https://a/cb", challenge, "S256")
        shouldThrow<ResponseStatusException> {
            service.exchangeCode(code, "oac_B", null, "https://b/cb", verifier)
        }
    }

    @Test
    fun `access token 검증 - 정상`() {
        val (service, _, tokenRepo) = freshService()
        val userId: UUID = idFactory.newUuid()
        val hash = MessageDigest.getInstance("SHA-256").digest("the-access-token".toByteArray(Charsets.UTF_8))
        val token =
            OAuthToken(
                id = idFactory.newUuid(),
                userId = userId,
                clientId = "oac_x",
                accessHash = hash,
                refreshHash = null,
                expiresAt =
                    java.time.OffsetDateTime
                        .now()
                        .plusMinutes(30),
            )
        every { tokenRepo.findByAccessHashAndRevokedAtIsNull(any()) } returns token
        val resolved = service.authenticateAccessToken("the-access-token")
        check(resolved == userId)
    }
}
