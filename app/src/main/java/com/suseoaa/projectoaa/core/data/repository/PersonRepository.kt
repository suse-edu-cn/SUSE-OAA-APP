package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.core.network.model.person.Data
import com.suseoaa.projectoaa.core.network.person.PersonService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val api: PersonService,
    private val tokenManager: TokenManager,
    private val cookieJar: SchoolCookieJar,
    private val database: CourseDatabase
) {
    // 退出登录逻辑
    suspend fun logout() {
        // 1. 清除 Token (本地持久化存储的凭证)
        tokenManager.clearToken()

        // 2. 清除 Cookie (内存中的 Session)
        cookieJar.clear()
    }

    suspend fun getPersonInfo(): Result<Data> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPersonInfo()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 200) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "获取用户信息失败"))
                }
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 修改密码
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = ChangePasswordRequest(
                oldpassword = oldPassword,
                password = newPassword
            )
            val response = api.changePassword(request)

            if (response.isSuccessful) {
                val body = response.body()
                // 根据提供的响应体：code 为 200 即成功
                if (body != null && body.code == 200) {
                    Result.success(body.message)
                } else {
                    Result.failure(Exception(body?.message ?: "修改失败"))
                }
            } else {
                // 处理 HTTP 错误 (如 400, 500)
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("请求失败(${response.code()}): $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}