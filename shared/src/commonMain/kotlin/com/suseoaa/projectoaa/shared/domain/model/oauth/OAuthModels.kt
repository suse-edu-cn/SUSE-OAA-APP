package com.suseoaa.projectoaa.shared.domain.model.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 已知标准 OAuth Scope 选项。 */
object OAuthKnownScopes {
    val all = listOf("openid", "profile", "email", "level")
}

/** GET /oauth/client/public 的单个权限说明项。 */
@Serializable
data class OAuthScopeOption(
    @SerialName("key") val key: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = ""
)

/** GET /oauth/client/public 返回的公开客户端信息。 */
@Serializable
data class OAuthClientPublicInfo(
    @SerialName("client_id") val clientId: String = "",
    @SerialName("client_name") val clientName: String = "",
    @SerialName("scopes") val scopes: List<OAuthScopeOption> = emptyList()
)

/** GET /oauth/authorize 请求参数。 */
data class OAuthAuthorizeRequest(
    val clientId: String,
    val redirectUri: String,
    val responseType: String = "code",
    val scope: String = "",
    val state: String = ""
)

/** GET /oauth/authorize 从 302 Location 解析出的授权回调结果。 */
data class OAuthAuthorizeResult(
    val redirectUrl: String,
    val code: String = "",
    val state: String = ""
)

/** GET /app/list、GET /app/detail 及 POST /app/create 返回的应用实体。 */
@Serializable
data class OaaApp(
    @SerialName("client_id") val clientId: String = "",
    @SerialName("mask_secret") val maskSecret: String = "",
    @SerialName("client_secret") val clientSecret: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("redirect_uris") val redirectUris: String = "",
    @SerialName("scope") val scope: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val displaySecret: String get() = clientSecret.ifBlank { maskSecret }
}

// 请求体不设默认值，确保全局 Json 未开启 encodeDefaults 时空串与 false 也会被序列化。

/** POST /app/create */
@Serializable
data class CreateAppRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("name") val name: String,
    @SerialName("redirect_uris") val redirectUris: String,
    @SerialName("scope") val scope: String
)

/** POST /app/update */
@Serializable
data class UpdateAppRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("name") val name: String,
    @SerialName("redirect_uris") val redirectUris: String,
    @SerialName("scope") val scope: String,
    @SerialName("is_active") val isActive: Boolean
)

/** POST /app/delete */
@Serializable
data class DeleteAppRequest(
    @SerialName("client_id") val clientId: String
)
