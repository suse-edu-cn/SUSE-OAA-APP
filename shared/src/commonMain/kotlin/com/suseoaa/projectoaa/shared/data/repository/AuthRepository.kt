package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.api.AuthApi
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterResponse

/**
 * 认证仓库
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    /**
     * 登录
     */
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username = username, password = password)
            val response = authApi.login(request)
            
            if (response.code == 200 && response.data != null) {
                // 保存 Token 和学号
                tokenManager.saveToken(response.data.token)
                tokenManager.saveCurrentStudentId(username)
                Result.Success(response)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("网络请求失败: ${e.message}", exception = e)
        }
    }

    /**
     * 注册
     */
    suspend fun register(username: String, password: String, confirmPassword: String): Result<RegisterResponse> {
        return try {
            val request = RegisterRequest(
                username = username,
                password = password,
                confirmPassword = confirmPassword
            )
            val response = authApi.register(request)
            
            if (response.code == 200) {
                Result.Success(response)
            } else {
                Result.Error(response.message, response.code)
            }
        } catch (e: Exception) {
            Result.Error("注册失败: ${e.message}", exception = e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        tokenManager.clearToken()
    }

    /**
     * 获取当前 Token
     */
    fun getToken() = tokenManager.tokenFlow

    /**
     * 是否已登录
     */
    fun isLoggedIn() = tokenManager.isLoggedIn

    /**
     * 获取当前学生 ID
     */
    fun getCurrentStudentId() = tokenManager.currentStudentId
}
