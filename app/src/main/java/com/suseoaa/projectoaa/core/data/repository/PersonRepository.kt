package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.dataStore.TokenManager
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
        // 1. 清除 Token
        tokenManager.clearToken()

        // 2. 清除 Cookie (Session)
        cookieJar.clear()

        // 3. 清空本地数据库
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
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
}