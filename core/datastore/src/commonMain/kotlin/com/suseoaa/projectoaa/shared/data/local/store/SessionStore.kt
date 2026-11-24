package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 登录会话：access token、refresh token、OAA 用户 ID、当前学号与登录态。
 */
class SessionStore(private val dataStore: DataStore<Preferences>) {

    /**
     * 供 Ktor 的鉴权拦截器同步取用——拦截器在构造 header 时不能挂起，
     * 所以这里保留一份内存快照，由 [tokenFlow] 与 [saveTokens] 维护。
     */
    @kotlin.concurrent.Volatile
    var cachedToken: String? = null
        private set

    val tokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        val token = prefs[Keys.USER_TOKEN]
        cachedToken = token
        token
    }

    /**
     * 当前学号。教务、考试、小组件等按它匹配教务账号，
     * 必须是真正的学号——v2 允许用用户名或邮箱登录，不能直接把登录账号写进来。
     */
    val currentStudentId: Flow<String?> = dataStore.data.map { it[Keys.CURRENT_STUDENT_ID] }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }

    suspend fun saveTokens(token: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_TOKEN] = token
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.IS_LOGGED_IN] = true
        }
        cachedToken = token
    }

    suspend fun getRefreshToken(): String? = dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun saveUserId(userId: Int) {
        dataStore.edit { it[Keys.USER_ID] = userId.toString() }
    }

    suspend fun getUserId(): Int? = dataStore.data.map { it[Keys.USER_ID]?.toIntOrNull() }.first()

    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { it[Keys.CURRENT_STUDENT_ID] = studentId }
    }

    suspend fun clearCurrentStudentId() {
        dataStore.edit { it.remove(Keys.CURRENT_STUDENT_ID) }
    }

    /**
     * v1 时代登录留下的会话：有 access token 却没有 refresh token。
     * v2 后端不认旧 token，这种会话需要清掉让用户重新登录。
     */
    suspend fun isLegacySession(): Boolean {
        val prefs = dataStore.data.first()
        return !prefs[Keys.USER_TOKEN].isNullOrEmpty() && prefs[Keys.REFRESH_TOKEN].isNullOrEmpty()
    }

    /** 清空会话，由 [com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner] 统一调用。 */
    /** 由 UserDataCleaner 调用；internal 会被模块边界挡住，故为 public。 */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.USER_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.CURRENT_STUDENT_ID)
            // v1 用来控制「每 10 天重新登录」的时间戳，v2 已不用，顺手清掉
            prefs.remove(Keys.LEGACY_TOKEN_LAST_UPDATE_TIME)
        }
        cachedToken = null
    }

    private object Keys {
        val USER_TOKEN = stringPreferencesKey("jwt_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val CURRENT_STUDENT_ID = stringPreferencesKey("current_student_id")
        val LEGACY_TOKEN_LAST_UPDATE_TIME = stringPreferencesKey("token_last_update_time")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
}
