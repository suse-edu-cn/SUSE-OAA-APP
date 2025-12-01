package com.suseoaa.projectoaa.navigation.repository

import android.content.Context
import android.content.SharedPreferences
import com.suseoaa.projectoaa.common.theme.ThemeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_PRIVACY_ENABLED = "privacy_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- 主题 ---
    override fun saveThemeName(themeName: String) {
        prefs.edit().putString(KEY_THEME_NAME, themeName).apply()
    }

    // 提供一个默认主题
    override fun getThemeName(): String {
        return prefs.getString(KEY_THEME_NAME, ThemeManager.themeList.first().name)
            ?: ThemeManager.themeList.first().name
    }

    // --- 通知 ---
    override fun saveNotificationEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, isEnabled).apply()
    }

    // 默认开启通知
    override fun getNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    // --- 隐私 ---
    override fun savePrivacyEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_ENABLED, isEnabled).apply()
    }

    // 默认关闭个性化
    override fun getPrivacyEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_ENABLED, true)
    }
}