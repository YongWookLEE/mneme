package com.mneme.oauth

import com.mneme.security.AuthenticatedUserResolver
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * OAuth 엔드포인트.
 *
 * - `POST /oauth/register` — RFC 7591 DCR. 공개.
 * - `GET /oauth/authorize` — 인가 코드 발급. 세션 인증 필요.
 * - `POST /oauth/token` — code/refresh → token. 공개(클라이언트 자격으로 인증).
 *
 * @author Mneme
 * @since phase 10
 */
@RestController
@RequestMapping("/oauth")
class OAuthController(
    private val service: OAuthService,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** RFC 7591 DCR. */
    @PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody body: RegisterRequest,
    ): RegisterResponse {
        val r = service.registerClient(body.redirectUris, body.clientName)
        return RegisterResponse(
            clientId = r.clientId,
            clientSecret = r.clientSecret,
            redirectUris = r.redirectUris,
            clientName = r.clientName,
            tokenEndpointAuthMethod = "client_secret_post",
            grantTypes = listOf("authorization_code", "refresh_token"),
            responseTypes = listOf("code"),
        )
    }

    /**
     * 인가 코드 발급. 세션 인증된 사용자만 호출 가능. 미인증이면 Spring Security가 OAuth 로그인으로 리다이렉트.
     * 인증 후 redirect_uri로 code+state 부착해 리다이렉트.
     */
    @GetMapping("/authorize")
    fun authorize(
        @RequestParam("response_type") responseType: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("state", required = false) state: String?,
        @RequestParam("code_challenge") codeChallenge: String,
        @RequestParam("code_challenge_method") codeChallengeMethod: String,
        @RequestParam("scope", required = false) scope: String?,
        response: HttpServletResponse,
    ) {
        require(responseType == "code") { "response_type must be code" }
        val userId = userResolver.currentUserId()
        val code = service.issueCode(userId, clientId, redirectUri, codeChallenge, codeChallengeMethod)
        val sep = if (redirectUri.contains('?')) '&' else '?'
        val location =
            buildString {
                append(redirectUri)
                append(sep)
                append("code=").append(code)
                if (state != null) {
                    append("&state=").append(state)
                }
            }
        response.status = HttpStatus.FOUND.value()
        response.setHeader("Location", location)
    }

    /** 토큰 발급(code/refresh). x-www-form-urlencoded. */
    @PostMapping("/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun token(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("code", required = false) code: String?,
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret", required = false) clientSecret: String?,
        @RequestParam("code_verifier", required = false) codeVerifier: String?,
        @RequestParam("refresh_token", required = false) refreshToken: String?,
    ): TokenResponse {
        val issued =
            when (grantType) {
                "authorization_code" -> {
                    require(code != null && redirectUri != null && codeVerifier != null) { "invalid_request" }
                    service.exchangeCode(code, clientId, clientSecret, redirectUri, codeVerifier)
                }
                "refresh_token" -> {
                    require(refreshToken != null) { "invalid_request" }
                    service.refresh(refreshToken, clientId, clientSecret)
                }
                else -> throw IllegalArgumentException("unsupported_grant_type: $grantType")
            }
        return TokenResponse(
            accessToken = issued.accessToken,
            tokenType = "Bearer",
            expiresIn = issued.expiresInSeconds,
            refreshToken = issued.refreshToken,
            scope = "mcp",
        )
    }

    /** DCR 요청 본문. */
    data class RegisterRequest(
        val redirectUris: List<String>,
        val clientName: String? = null,
    )

    /** DCR 응답 — client_secret은 1회만 노출. */
    data class RegisterResponse(
        val clientId: String,
        val clientSecret: String,
        val redirectUris: List<String>,
        val clientName: String?,
        val tokenEndpointAuthMethod: String,
        val grantTypes: List<String>,
        val responseTypes: List<String>,
    )

    /** Token 응답(RFC 6749 §5.1). */
    data class TokenResponse(
        val accessToken: String,
        val tokenType: String,
        val expiresIn: Long,
        val refreshToken: String,
        val scope: String,
    )
}
