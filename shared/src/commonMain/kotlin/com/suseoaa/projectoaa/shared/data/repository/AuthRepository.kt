package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.remote.api.OaaApiService
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginResponse
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterResponse

/**
 * 认证仓库
 */
class AuthRepository(
    private val authApi: OaaApiService,
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
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "登录失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 注册
     */
    suspend fun register(
        name: String,
        password: String,
        studentId: String,
        username: String,
        email: String
    ): Result<RegisterResponse> {
        return try {
            val request = RegisterRequest(
                name = name,
                password = password,
                studentId = studentId,
                username = username,
                email = email
            )
            val response = authApi.register(request)
            
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "注册失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        tokenManager.clearSession()
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
