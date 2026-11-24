package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaOAuthApi
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeResult
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthClientPublicInfo
import com.suseoaa.projectoaa.shared.domain.repository.OAuthRepository

/**
 * OAuth 授权仓库实现。
 */
class OAuthRepositoryImpl(
    private val api: OaaOAuthApi
) : OAuthRepository {

    override suspend fun getClientPublicInfo(
        clientId: String,
        scope: String?
    ): Result<OAuthClientPublicInfo> = api.clientPublic(clientId = clientId, scope = scope)

    override suspend fun authorize(request: OAuthAuthorizeRequest): Result<OAuthAuthorizeResult> =
        api.authorize(request)
}
