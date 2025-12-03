package com.suseoaa.projectoaa.login.repository

// [修复] 导入 Inject 和 Singleton
import javax.inject.Inject
import javax.inject.Singleton
// [修复] 移除了 NetworkModule
// import com.suseoaa.projectoaa.common.network.NetworkModule
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.LoginRequest
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import retrofit2.Response

// [修复] 1. 添加注解并通过构造函数注入
@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService // Hilt 将从 NetworkModule 提供这个
) {
    // [修复] 2. 移除了 'private val api = NetworkModule.createService(...)'

    suspend fun login(username: String, pass: String): Result<String> {
        val response = api.login(LoginRequest(username, pass))
        // [修复] 简化 handleResponse 逻辑
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data?.token ?: "")
        } else {
            Result.failure(Exception("登录失败: ${response.code()}"))
        }
    }

    suspend fun register(req: RegisterRequest): Result<String> {
        val response = api.register(req)
        return if (response.isSuccessful) {
            Result.success("注册成功")
        } else {
            Result.failure(Exception("注册失败: ${response.code()}"))
        }
    }

    suspend fun getUserInfo(token: String): Result<UserInfoData> {
        val response = api.getUserInfo(token)
        return if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
        } else {
            Result.failure(Exception("获取用户信息失败: ${response.code()}"))
        }
    }
}