package com.suseoaa.projectoaa.login.repository

import com.suseoaa.projectoaa.common.network.NetworkModule
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.LoginRequest
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import retrofit2.Response

class AuthRepository {
    // 获取 API 实例
    private val api = NetworkModule.createService(ApiService::class.java)

    suspend fun login(username: String, pass: String): Result<String> {
        val response = api.login(LoginRequest(username, pass))
        return handleResponse(response) { it.data?.token ?: "" }
    }

    suspend fun register(req: RegisterRequest): Result<String> {
        val response = api.register(req)
        return handleResponse(response) { "注册成功" }
    }

    suspend fun getUserInfo(token: String): Result<UserInfoData> {
        val response = api.getUserInfo(token)
        return handleResponse(response) { it.data!! }
    }

    // 统一处理 API 响应的辅助函数
    private fun <T, R> handleResponse(response: Response<T>, transform: (T) -> R): Result<R> {
        return if (response.isSuccessful && response.body() != null) {
            try {
                val body = response.body()!!
                // 假设后端 code 200 为成功，根据你的实际 API 调整
                // 这里简单判断：只要 HTTP 成功且 body 不为空即视为成功
                Result.success(transform(body))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("请求失败: ${response.code()} ${response.message()}"))
        }
    }
}