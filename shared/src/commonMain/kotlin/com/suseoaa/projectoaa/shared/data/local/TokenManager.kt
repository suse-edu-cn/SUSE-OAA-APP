package com.suseoaa.projectoaa.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    // 更新弹窗版本追踪
    val UPDATE_DIALOG_SHOWN_VERSION = stringPreferencesKey("update_dialog_shown_version")

    // 开学日期
    val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")

    // 是否存在第0周
    val SEMESTER_HAS_WEEK_ZERO = booleanPreferencesKey("semester_has_week_zero")

    // 652签到功能解锁
    val CHECKIN_UNLOCKED = booleanPreferencesKey("checkin_feature_unlocked")
}

internal const val DATA_STORE_FILE_NAME = "auth_prefs.preferences_pb"

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

    // ==================== Token & Auth ====================

    val tokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[PreferencesKeys.USER_TOKEN]
        cachedToken = token
        token
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.CURRENT_STUDENT_ID]
        cachedStudentId = id
        id
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TOKEN] = token
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
        }
        cachedToken = token
    }

    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STUDENT_ID] = studentId
        }
        cachedStudentId = studentId
    }

    suspend fun getTokenSynchronously(): String? {
        return tokenFlow.first()
    }

    // ==================== User GPA IDs ====================

    val userInfoFlow: Flow<Map<String, String?>> = dataStore.data.map { prefs ->
        mapOf(
            "jg_id" to prefs[PreferencesKeys.JG_ID],
            "zyh_id" to prefs[PreferencesKeys.ZYH_ID],
            "njdm_id" to prefs[PreferencesKeys.NJDM_ID]
        )
    }

    fun getUserGpaIds(): Flow<Triple<String?, String?, String?>> = dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.JG_ID],
            prefs[PreferencesKeys.ZYH_ID],
            prefs[PreferencesKeys.NJDM_ID]
        )
    }

    suspend fun saveUserInfo(jgId: String, zyhId: String, njdmId: String) {
        dataStore.edit { prefs ->
            if (jgId.isNotEmpty()) prefs[PreferencesKeys.JG_ID] = jgId
            if (zyhId.isNotEmpty()) prefs[PreferencesKeys.ZYH_ID] = zyhId
            if (njdmId.isNotEmpty()) prefs[PreferencesKeys.NJDM_ID] = njdmId
        }
    }

    /** Alias for [saveUserInfo] */
    suspend fun saveUserGpaIds(jgId: String, zyhId: String, njdmId: String) =
        saveUserInfo(jgId, zyhId, njdmId)

    // ==================== Theme ====================

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    // ==================== 更新弹窗状态管理 ====================

    val updateDialogShownVersionFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.UPDATE_DIALOG_SHOWN_VERSION]
    }

    suspend fun hasShownUpdateDialogForVersion(version: String): Boolean {
        val shownVersion = updateDialogShownVersionFlow.first()
        return shownVersion == version
    }

    suspend fun markUpdateDialogShown(version: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.UPDATE_DIALOG_SHOWN_VERSION] = version
        }
    }

    // ==================== 开学日期管理 ====================

    val semesterStartDateFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SEMESTER_START_DATE]
    }

    suspend fun saveSemesterStartDate(dateString: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEMESTER_START_DATE] = dateString
        }
    }

    suspend fun getSemesterStartDate(): String? {
        return semesterStartDateFlow.first()
    }

    val semesterHasWeekZeroFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SEMESTER_HAS_WEEK_ZERO] ?: false
    }

    suspend fun saveSemesterHasWeekZero(hasWeekZero: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEMESTER_HAS_WEEK_ZERO] = hasWeekZero
        }
    }

    suspend fun getSemesterHasWeekZero(): Boolean {
        return semesterHasWeekZeroFlow.first()
    }

    // ==================== 652签到功能解锁状态 ====================

    val checkinUnlockedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CHECKIN_UNLOCKED] ?: false
    }

    suspend fun unlockCheckinFeature() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CHECKIN_UNLOCKED] = true
        }
    }

    suspend fun isCheckinUnlocked(): Boolean {
        return checkinUnlockedFlow.first()
    }

    // ==================== 清除数据 ====================

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_TOKEN)
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
        }
        cachedToken = null
    }

    /** 清除所有数据 */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        cachedToken = null
        cachedStudentId = null
    }

    /** Alias for [clear] */
    suspend fun clearAll() = clear()
}
