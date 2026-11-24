package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp

/**
 * AppManagementRepository 的契约：OAuth 应用管理（管理员使用）。
 */
interface AppManagementRepository {

    suspend fun listApps(): Result<List<OaaApp>>

    suspend fun getAppDetail(clientId: String): Result<OaaApp>

    suspend fun createApp(
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String
    ): Result<OaaApp>

    suspend fun updateApp(
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String,
        isActive: Boolean
    ): Result<Unit>

    suspend fun deleteApp(clientId: String): Result<Unit>
}
