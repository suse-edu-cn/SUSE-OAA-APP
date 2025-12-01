package com.suseoaa.projectoaa.common.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"

    var jwtToken: String? = null
    var currentUser by mutableStateOf<String?>("游客")
    var currentRole by mutableStateOf<String?>("未登录")

    fun saveToken(context: Context, token: String) {
        jwtToken = token
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveUserInfo(context: Context, username: String, role: String) {
        currentUser = username
        currentRole = role

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun fetchToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (jwtToken == null) {
            jwtToken = prefs.getString(KEY_TOKEN, null)
        }

        val savedUser = prefs.getString(KEY_USERNAME, null)
        val savedRole = prefs.getString(KEY_ROLE, null)

        if (savedUser != null) currentUser = savedUser
        if (savedRole != null) currentRole = savedRole

        return jwtToken
    }

    fun clear(context: Context) {
        jwtToken = null
        currentUser = null
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(): Boolean {
        return !jwtToken.isNullOrBlank()
    }
}