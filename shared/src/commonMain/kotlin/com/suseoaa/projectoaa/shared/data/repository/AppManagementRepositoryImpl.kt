package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAppApi
import com.suseoaa.projectoaa.shared.domain.model.oauth.CreateAppRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.DeleteAppRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp
import com.suseoaa.projectoaa.shared.domain.model.oauth.UpdateAppRequest
import com.suseoaa.projectoaa.shared.domain.repository.AppManagementRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * OAuth 应用管理仓库实现。
 */
class AppManagementRepositoryImpl(
    private val api: OaaAppApi,
    private val json: Json
) : AppManagementRepository {

    override suspend fun listApps(): Result<List<OaaApp>> =
        api.listApps().map { it.orEmpty() }

    override suspend fun getAppDetail(clientId: String): Result<OaaApp> =
        api.appDetail(clientId)

    override suspend fun createApp(
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String
    ): Result<OaaApp> {
        val element = api.createApp(
            CreateAppRequest(
                clientId = clientId,
                name = name,
                redirectUris = redirectUris,
                scope = scope
            )
        ).getOrElse { return Result.failure(it) }
        return Result.success(parseCreatedApp(element, clientId, name, redirectUris, scope))
    }

    override suspend fun updateApp(
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String,
        isActive: Boolean
    ): Result<Unit> = api.updateApp(
        UpdateAppRequest(
            clientId = clientId,
            name = name,
            redirectUris = redirectUris,
            scope = scope,
            isActive = isActive
        )
    )

    override suspend fun deleteApp(clientId: String): Result<Unit> =
        api.deleteApp(DeleteAppRequest(clientId = clientId))

    private fun parseCreatedApp(
        element: JsonElement?,
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String
    ): OaaApp = when (element) {
        is JsonObject -> {
            val parsed = runCatching { json.decodeFromJsonElement<OaaApp>(element) }.getOrNull() ?: OaaApp()
            val altSecret = (element["secret"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            parsed.copy(
                clientId = parsed.clientId.ifBlank { clientId },
                name = parsed.name.ifBlank { name },
                redirectUris = parsed.redirectUris.ifBlank { redirectUris },
                scope = parsed.scope.ifBlank { scope },
                clientSecret = parsed.clientSecret.ifBlank { altSecret }
            )
        }
        is JsonPrimitive -> OaaApp(
            clientId = clientId,
            name = name,
            redirectUris = redirectUris,
            scope = scope,
            clientSecret = element.contentOrNull.orEmpty()
        )
        else -> OaaApp(
            clientId = clientId,
            name = name,
            redirectUris = redirectUris,
            scope = scope
        )
    }
}
