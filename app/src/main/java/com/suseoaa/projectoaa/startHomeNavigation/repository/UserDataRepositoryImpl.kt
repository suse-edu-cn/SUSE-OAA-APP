package com.suseoaa.projectoaa.navigation.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
class UserDataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context) :
    UserDataRepository {

    companion object {
        private const val PREFS_NAME = "check_in_prefs"
        private const val KEY_LAST_CHECK_IN = "last_check_in_date"
        private const val KEY_IMAGE_URL = "daily_image_url"
        private const val KEY_IMAGE_DATE = "daily_image_date"
        private const val KEY_CHECK_IN_COUNT = "check_in_count"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLastCheckInDate(): String? {
        return prefs.getString(KEY_LAST_CHECK_IN, null)
    }

    override fun saveCheckInDate(date: String) {
        prefs.edit().putString(KEY_LAST_CHECK_IN, date).apply()
    }

    // 实现了签到次数的持久化
    override fun getCheckInCount(): Int {
        return prefs.getInt(KEY_CHECK_IN_COUNT, 12) // 默认值 12
    }

    override fun saveCheckInCount(count: Int) {
        prefs.edit().putInt(KEY_CHECK_IN_COUNT, count).apply()
    }

    override fun getCachedImage(): Pair<String?, String?> {
        val date = prefs.getString(KEY_IMAGE_DATE, null)
        val url = prefs.getString(KEY_IMAGE_URL, null)
        return Pair(date, url)
    }

    override fun saveCachedImage(date: String, url: String) {
        prefs.edit()
            .putString(KEY_IMAGE_DATE, date)
            .putString(KEY_IMAGE_URL, url)
            .apply()
    }
}