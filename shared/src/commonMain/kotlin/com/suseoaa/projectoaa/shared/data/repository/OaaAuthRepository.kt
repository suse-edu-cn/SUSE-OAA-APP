package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse

/**
 * OAA 后端登录仓库
 */
class OaaAuthRepository(
    private val api: OaaApiService
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username = username, password = password)
            val response = api.login(request)

            if (response.code == 200 && response.data?.token != null) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "登录失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
