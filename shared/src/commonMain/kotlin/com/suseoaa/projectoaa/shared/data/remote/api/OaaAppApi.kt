package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.oauth.CreateAppRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.DeleteAppRequest
import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp
import com.suseoaa.projectoaa.shared.domain.model.oauth.UpdateAppRequest
import kotlinx.serialization.json.JsonElement

/** `/app` 下的接口：OAuth 应用管理（管理员使用）。 */
class OaaAppApi(private val requester: OaaRequester) {

    suspend fun listApps(): Result<List<OaaApp>?> =
        requester.get("/app/list", "获取应用列表失败")

    suspend fun appDetail(clientId: String): Result<OaaApp> =
        requester.get(
            "/app/detail",
            "获取应用详情失败",
            params = mapOf("client_id" to clientId)
        )

    suspend fun createApp(request: CreateAppRequest): Result<JsonElement?> =
        requester.post("/app/create", request, "创建应用失败")

    suspend fun updateApp(request: UpdateAppRequest): Result<Unit> =
        requester.post("/app/update", request, "修改应用失败")

    suspend fun deleteApp(request: DeleteAppRequest): Result<Unit> =
        requester.post("/app/delete", request, "删除应用失败")
}
