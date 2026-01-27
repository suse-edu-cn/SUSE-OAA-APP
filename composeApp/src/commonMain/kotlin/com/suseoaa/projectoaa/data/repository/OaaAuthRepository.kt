package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.OaaApiService
import com.suseoaa.projectoaa.data.model.LoginRequest
import com.suseoaa.projectoaa.data.model.LoginResponse

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
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
