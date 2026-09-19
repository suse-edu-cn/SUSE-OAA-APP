package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository

/**
 * OAA 后端登录仓库
 */
class OaaAuthRepositoryImpl(
    private val api: OaaApiService
) : OaaAuthRepository {
    override suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username = username, password = password)
            val response = api.login(request)

            if (response.code == 200 && response.data?.token != null) {
                Result.success(response)
            } else {
                appFailure(AppError.Business(userMessage = response.message.ifEmpty { "登录失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
