package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.data.remote.network.OaaAuthenticator
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeResult
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthClientPublicInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException

/**
 * `/oauth` 下的接口。
 *
 * `/oauth/authorize` 仅识别 `Cookie: token=...`，且会返回 HTTP 302 重定向到回调地址（或未登录时跳回 `/auth`），
 * 因此必须使用关闭自动重定向的客户端手动解析 `Location` 头，并在必要时刷新 token 后重试一次。
 */
class OaaOAuthApi(
    private val requester: OaaRequester,
    private val noRedirectClient: HttpClient,
    private val authenticator: OaaAuthenticator
) {

    suspend fun clientPublic(clientId: String, scope: String? = null): Result<OAuthClientPublicInfo> =
        requester.get(
            "/oauth/client/public",
            "获取应用授权信息失败",
            params = mapOf(
                "client_id" to clientId,
                "scope" to scope?.trim()?.ifBlank { null }
            )
        )

    suspend fun authorize(request: OAuthAuthorizeRequest): Result<OAuthAuthorizeResult> {
        val initialToken = authenticator.currentAccessToken()?.takeIf { it.isNotBlank() }
            ?: return appFailure(AppError.SessionExpired)
        return try {
            var response = executeAuthorize(initialToken, request)
            val initialLocation = response.redirectLocation()
            if ((initialLocation != null && isAuthRedirect(initialLocation)) ||
                response.status == HttpStatusCode.Unauthorized
            ) {
                val refreshedToken = authenticator.refreshAccessToken(initialToken)
                    ?: return appFailure(AppError.SessionExpired)
                response = executeAuthorize(refreshedToken, request)
            }

            val finalLocation = response.redirectLocation()
            if (finalLocation != null) {
                if (isAuthRedirect(finalLocation)) {
                    return appFailure(AppError.SessionExpired)
                }
                val parsedUrl = runCatching { Url(finalLocation) }.getOrNull()
                val oauthError = parsedUrl?.parameters?.get("error")?.takeIf { it.isNotBlank() }
                if (oauthError != null) {
                    val description = parsedUrl.parameters["error_description"]
                        ?.takeIf { it.isNotBlank() } ?: oauthError
                    return appFailure(AppError.Business(userMessage = description))
                }
                return Result.success(
                    OAuthAuthorizeResult(
                        redirectUrl = finalLocation,
                        code = parsedUrl?.parameters?.get("code").orEmpty(),
                        state = parsedUrl?.parameters?.get("state").orEmpty()
                    )
                )
            }

            val bodyText = response.bodyAsText()
            appFailure(requester.errorFromResponse(response.status, bodyText, "OAuth 授权失败"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            appFailure(AppError.Network(e))
        }
    }

    private suspend fun executeAuthorize(token: String, request: OAuthAuthorizeRequest): HttpResponse =
        noRedirectClient.get(ApiConfig.OAA_BASE + "/oauth/authorize") {
            header(HttpHeaders.Cookie, "token=$token")
            parameter("client_id", request.clientId)
            parameter("redirect_uri", request.redirectUri)
            parameter("response_type", request.responseType)
            request.scope.trim().takeIf { it.isNotEmpty() }?.let { parameter("scope", it) }
            request.state.trim().takeIf { it.isNotEmpty() }?.let { parameter("state", it) }
        }

    private fun HttpResponse.redirectLocation(): String? =
        if (status.value in 300..399) {
            headers[HttpHeaders.Location]?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }

    private fun isAuthRedirect(location: String): Boolean =
        location.startsWith("${ApiConfig.OAA_WEB_ORIGIN}/auth") || location.startsWith("/auth")
}
