package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeResult
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthClientPublicInfo

/**
 * OAuthRepository 的契约：查询应用公开信息与执行 OAuth 授权。
 */
interface OAuthRepository {

    suspend fun getClientPublicInfo(clientId: String, scope: String? = null): Result<OAuthClientPublicInfo>

    suspend fun authorize(request: OAuthAuthorizeRequest): Result<OAuthAuthorizeResult>
}
