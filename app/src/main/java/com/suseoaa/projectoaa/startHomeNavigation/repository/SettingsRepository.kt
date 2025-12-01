package com.suseoaa.projectoaa.navigation.repository

interface SettingsRepository {
    // --- 主题 ---
    fun saveThemeName(themeName: String)
    fun getThemeName(): String

    // --- 通知 ---
    fun saveNotificationEnabled(isEnabled: Boolean)
    fun getNotificationEnabled(): Boolean

    // --- 隐私 ---
    fun savePrivacyEnabled(isEnabled: Boolean)
    fun getPrivacyEnabled(): Boolean
}