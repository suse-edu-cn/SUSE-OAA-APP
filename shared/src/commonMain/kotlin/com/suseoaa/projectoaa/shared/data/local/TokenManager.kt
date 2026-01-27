package com.suseoaa.projectoaa.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/**
 * DataStore 偏好设置 Keys
 */
object PreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("jwt_token")
    val USER_ID = stringPreferencesKey("user_id")
    val CURRENT_STUDENT_ID = stringPreferencesKey("current_student_id")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    
    // 用户绩点计算相关
    val JG_ID = stringPreferencesKey("user_jg_id")
    val ZYH_ID = stringPreferencesKey("user_zyh_id")
    val NJDM_ID = stringPreferencesKey("user_njdm_id")
    
    // 设置
    val THEME_MODE = stringPreferencesKey("theme_mode")
}

internal const val DATA_STORE_FILE_NAME = "projectoaa_prefs.preferences_pb"

/**
 * 创建 DataStore
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}

/**
 * Token 管理器 - 跨平台
 */
class TokenManager(private val dataStore: DataStore<Preferences>) {
    
    // 内存缓存
    @kotlin.concurrent.Volatile
    var cachedToken: String? = null
        private set
    
    @kotlin.concurrent.Volatile
    var cachedStudentId: String? = null
        private set

    /**
     * Token Flow
     */
    val tokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[PreferencesKeys.USER_TOKEN]
        cachedToken = token
        token
    }

    /**
     * 当前学生 ID Flow
     */
    val currentStudentId: Flow<String?> = dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.CURRENT_STUDENT_ID]
        cachedStudentId = id
        id
    }

    /**
     * 是否已登录
     */
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    /**
     * 主题模式
     */
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    /**
     * 保存 Token
     */
    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TOKEN] = token
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
        }
        cachedToken = token
    }

    /**
     * 保存当前学生 ID
     */
    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STUDENT_ID] = studentId
        }
        cachedStudentId = studentId
    }

    /**
     * 保存用户绩点计算相关 ID
     */
    suspend fun saveUserGpaIds(jgId: String, zyhId: String, njdmId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.JG_ID] = jgId
            preferences[PreferencesKeys.ZYH_ID] = zyhId
            preferences[PreferencesKeys.NJDM_ID] = njdmId
        }
    }

    /**
     * 获取用户绩点计算 IDs
     */
    fun getUserGpaIds(): Flow<Triple<String?, String?, String?>> = dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.JG_ID],
            prefs[PreferencesKeys.ZYH_ID],
            prefs[PreferencesKeys.NJDM_ID]
        )
    }

    /**
     * 保存主题模式
     */
    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    /**
     * 清除 Token
     */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_TOKEN)
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
        }
        cachedToken = null
    }

    /**
     * 清除所有数据
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        cachedToken = null
        cachedStudentId = null
    }
}
