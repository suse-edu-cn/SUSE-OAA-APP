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
 * 登录会话：JWT、当前学号、登录态与 token 刷新时间。
 */
class SessionStore(private val dataStore: DataStore<Preferences>) {

    /**
     * 供 Ktor 的鉴权拦截器同步取用——拦截器在构造 header 时不能挂起，
     * 所以这里保留一份内存快照，由 [tokenFlow] 与 [saveToken] 维护。
     */
    @kotlin.concurrent.Volatile
    var cachedToken: String? = null
        private set

    val tokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        val token = prefs[Keys.USER_TOKEN]
        cachedToken = token
        token
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { it[Keys.CURRENT_STUDENT_ID] }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }

    suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_TOKEN] = token
            prefs[Keys.IS_LOGGED_IN] = true
        }
        cachedToken = token
    }

    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { it[Keys.CURRENT_STUDENT_ID] = studentId }
    }

    suspend fun saveTokenLastUpdateTime(timestamp: Long) {
        dataStore.edit { it[Keys.TOKEN_LAST_UPDATE_TIME] = timestamp.toString() }
    }

    suspend fun getTokenLastUpdateTime(): Long =
        dataStore.data.map { it[Keys.TOKEN_LAST_UPDATE_TIME]?.toLongOrNull() ?: 0L }.first()

    /** 清空会话，由 [com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner] 统一调用。 */
    /** 由 UserDataCleaner 调用；internal 会被模块边界挡住，故为 public。 */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.USER_TOKEN)
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.CURRENT_STUDENT_ID)
            prefs.remove(Keys.TOKEN_LAST_UPDATE_TIME)
        }
        cachedToken = null
    }

    private object Keys {
        val USER_TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = stringPreferencesKey("user_id")
        val CURRENT_STUDENT_ID = stringPreferencesKey("current_student_id")
        val TOKEN_LAST_UPDATE_TIME = stringPreferencesKey("token_last_update_time")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
}
