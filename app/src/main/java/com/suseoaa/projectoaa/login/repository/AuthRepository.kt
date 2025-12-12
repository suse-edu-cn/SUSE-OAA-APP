package com.suseoaa.projectoaa.login.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.LoginRequest
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "auth_token"
    }

    /**
     * 登录：获取 Token 并保存
     */
    suspend fun login(username: String, pass: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(username, pass))
            if (response.isSuccessful && response.body() != null) {
                // 里的 token
                val token = response.body()!!.data?.token ?: ""

                if (token.isNotEmpty()) {
                    saveToken(token)
                    return Result.success(token)
                }
                Result.failure(Exception("Token 为空"))
            } else {
                Result.failure(Exception("登录失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取用户信息：适配后端数据结构，提取 student_id
     */
    suspend fun getUserInfo(token: String): Result<UserInfoData> {
        return try {
            val response = api.getUserInfo(token)
            //
            if (response.isSuccessful && response.body()?.data != null) {
                val userInfo = response.body()!!.data!!

                // 将后端的 student_id (Long) 转为 String 保存为本地 UserID
                // 这是多账号打卡隔离的基石
                saveUserId(userInfo.student_id.toString())

                Result.success(userInfo)
            } else {
                Result.failure(Exception("获取用户信息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(req: RegisterRequest): Result<String> {
        return try {
            val response = api.register(req)
            if (response.isSuccessful) Result.success("注册成功")
            else Result.failure(Exception("注册失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 本地状态管理
    // ==========================================

    fun getCurrentUserId(): String {
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    private fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        Log.d("AuthRepository", "Current UserID updated: $userId")
    }

    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}