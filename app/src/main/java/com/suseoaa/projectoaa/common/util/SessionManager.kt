package com.suseoaa.projectoaa.common.util

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 1. 从 'object' 变为 Hilt 管理的 '@Singleton class'。
 * 2. 通过构造函数注入 SharedPreferences，移除所有 Context 依赖。
 */
@Singleton
class SessionManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        const val PREF_NAME = "user_session"
        const val KEY_TOKEN = "jwt_token"
        const val KEY_USERNAME = "username"
        const val KEY_ROLE = "role"
    }

    // 状态变量，设为 private set，只能由本类修改
    var jwtToken: String? = null
        private set
    var currentUser by mutableStateOf<String?>("游客")
        private set
    var currentRole by mutableStateOf<String?>("未登录")
        private set

    /**
     * Hilt 创建这个单例时，立即从 SharedPreferences 加载会话。
     * 您不再需要从 MainActivity 手动调用 fetchToken()。
     */
    init {
        loadSession()
    }

    fun saveToken(token: String) {
        jwtToken = token
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveUserInfo(username: String, role: String) {
        currentUser = username
        currentRole = role

        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply()
    }

    private fun loadSession() {
        if (jwtToken == null) {
            jwtToken = prefs.getString(KEY_TOKEN, null)
        }

        val savedUser = prefs.getString(KEY_USERNAME, null)
        val savedRole = prefs.getString(KEY_ROLE, null)

        if (savedUser != null) currentUser = savedUser
        if (savedRole != null) currentRole = savedRole
    }

    fun clear() {
        jwtToken = null
        currentUser = "游客"
        currentRole = "未登录"
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !jwtToken.isNullOrBlank()
    }
}