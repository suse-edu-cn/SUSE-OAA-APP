package com.suseoaa.projectoaa.core.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_CURRENT_STUDENT_ID = stringPreferencesKey("current_student_id")
        private val KEY_JG_ID = stringPreferencesKey("user_jg_id")
        private val KEY_ZYH_ID = stringPreferencesKey("user_zyh_id")
        private val KEY_NJDM_ID = stringPreferencesKey("user_njdm_id")
        // 用于记录更新弹窗是否已经显示过（针对特定版本）
        private val KEY_UPDATE_DIALOG_SHOWN_VERSION = stringPreferencesKey("update_dialog_shown_version")
        // 开学日期
        private val KEY_SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        // 652签到功能是否已解锁
        private val KEY_CHECKIN_UNLOCKED = booleanPreferencesKey("checkin_feature_unlocked")
    }

    // 内存缓存 (使用 kotlinx.atomicfu 或简单的 var 实现线程安全)
    var cachedToken: String? = null
        private set

    var cachedStudentId: String? = null
        private set

    val tokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[KEY_TOKEN]
        cachedToken = token
        token
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { preferences ->
        val id = preferences[KEY_CURRENT_STUDENT_ID]
        cachedStudentId = id
        id
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
        cachedToken = token
    }

    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENT_STUDENT_ID] = studentId
        }
        cachedStudentId = studentId
    }

    suspend fun getTokenSynchronously(): String? {
        return tokenFlow.first()
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        cachedToken = null
        cachedStudentId = null
    }

    val userInfoFlow: Flow<Map<String, String?>> = dataStore.data.map { prefs ->
        mapOf(
            "jg_id" to prefs[KEY_JG_ID],
            "zyh_id" to prefs[KEY_ZYH_ID],
            "njdm_id" to prefs[KEY_NJDM_ID]
        )
    }

    suspend fun saveUserInfo(jgId: String, zyhId: String, njdmId: String) {
        dataStore.edit { prefs ->
            if (jgId.isNotEmpty()) prefs[KEY_JG_ID] = jgId
            if (zyhId.isNotEmpty()) prefs[KEY_ZYH_ID] = zyhId
            if (njdmId.isNotEmpty()) prefs[KEY_NJDM_ID] = njdmId
        }
    }
    
    // ==================== 更新弹窗状态管理 ====================
    
    /**
     * 获取已经显示过更新弹窗的版本号
     */
    val updateDialogShownVersionFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_UPDATE_DIALOG_SHOWN_VERSION]
    }
    
    /**
     * 检查是否已经为特定版本显示过更新弹窗
     */
    suspend fun hasShownUpdateDialogForVersion(version: String): Boolean {
        val shownVersion = updateDialogShownVersionFlow.first()
        return shownVersion == version
    }
    
    /**
     * 标记已经为特定版本显示过更新弹窗
     */
    suspend fun markUpdateDialogShown(version: String) {
        dataStore.edit { prefs ->
            prefs[KEY_UPDATE_DIALOG_SHOWN_VERSION] = version
        }
    }
    
    // ==================== 开学日期管理 ====================
    
    /**
     * 获取保存的开学日期
     */
    val semesterStartDateFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SEMESTER_START_DATE]
    }
    
    /**
     * 保存开学日期
     */
    suspend fun saveSemesterStartDate(dateString: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SEMESTER_START_DATE] = dateString
        }
    }
    
    /**
     * 同步获取开学日期
     */
    suspend fun getSemesterStartDate(): String? {
        return semesterStartDateFlow.first()
    }
    
    // ==================== 652签到功能解锁状态 ====================
    
    /**
     * 652签到功能是否已解锁
     */
    val checkinUnlockedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CHECKIN_UNLOCKED] ?: false
    }
    
    /**
     * 解锁652签到功能（永久保存）
     */
    suspend fun unlockCheckinFeature() {
        dataStore.edit { prefs ->
            prefs[KEY_CHECKIN_UNLOCKED] = true
        }
    }
    
    /**
     * 检查652签到功能是否已解锁
     */
    suspend fun isCheckinUnlocked(): Boolean {
        return checkinUnlockedFlow.first()
    }
}

