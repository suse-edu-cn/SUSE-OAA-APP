package com.suseoaa.projectoaa.startHomeNavigation.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserDataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    // private val apiService: ApiService // TODO: 将来在这里注入 ApiService
) : UserDataRepository {

    companion object {
        private const val PREFS_NAME = "check_in_prefs"
        private const val KEY_LAST_CHECK_IN = "last_check_in_date"
        private const val KEY_CHECK_IN_COUNT = "check_in_count"
        private const val KEY_IMAGE_URL = "daily_image_url"
        private const val KEY_IMAGE_DATE = "daily_image_date"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getLastCheckInDate(userId: String): String? {
        return withContext(Dispatchers.IO) {
            // 1. 优先返回本地缓存(保证速度)
            val localDate = prefs.getString("${KEY_LAST_CHECK_IN}_$userId", null)
            // TODO:未来可以在这里添加逻辑：如果本地为空，尝试从网络获取
            // if (localDate == null) { return apiService.getCheckInDate(userId) }
            localDate
        }
    }

    override suspend fun saveCheckInDate(userId: String, date: String) {
        withContext(Dispatchers.IO) {
// 保存到本地 (立即生效，保证 UI 响应快 - 乐观更新)
            prefs.edit().putString("${KEY_LAST_CHECK_IN}_$userId", date).apply()
// TODO: 异步上传到后端
            // try {
            //     apiService.uploadCheckInDate(userId, date)
            // } catch (e: Exception) {
            //     //处理上传失败，下次启动时重试
            // }
        }
    }

    override suspend fun getCheckInCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("${KEY_CHECK_IN_COUNT}_$userId", 0)
        }
    }

    override suspend fun saveCheckInCount(userId: String, count: Int) {
        withContext(Dispatchers.IO) {
            //本地保存
            prefs.edit().putInt("${KEY_CHECK_IN_COUNT}_$userId", count).apply()

            // TODO: 后端同步
            // apiService.updateCheckInCount(userId, count)
        }
    }

    override suspend fun getCachedImage(): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            val date = prefs.getString(KEY_IMAGE_DATE, null)
            val url = prefs.getString(KEY_IMAGE_URL, null)
            Pair(date, url)
        }
    }

    override suspend fun saveCachedImage(date: String, url: String) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putString(KEY_IMAGE_DATE, date)
                .putString(KEY_IMAGE_URL, url)
                .apply()
        }
    }

    // [预留]同步接口实现
    override suspend fun syncUserData(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            // TODO: 这里写完整的同步逻辑
            // val remoteData = apiService.getUserData(userId)
            // saveCheckInDate(userId, remoteData.date)
            // saveCheckInCount(userId, remoteData.count)
            true //暂时返回成功
        }
    }
}