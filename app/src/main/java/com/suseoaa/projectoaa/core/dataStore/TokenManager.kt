package com.suseoaa.projectoaa.core.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 1. 定义 DataStore 的扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // 定义 Key
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")

        // 存储当前选中的学生学号
        private val CURRENT_STUDENT_ID_KEY = stringPreferencesKey("current_student_id")

        // 用户信息 Key (用于绩点计算)
        private val JG_ID_KEY = stringPreferencesKey("user_jg_id")
        private val ZYH_ID_KEY = stringPreferencesKey("user_zyh_id")
        private val NJDM_ID_KEY = stringPreferencesKey("user_njdm_id")
    }

    // 内存缓存，使用 @Volatile 保证线程可见性
    @Volatile
    var cachedToken: String? = null
        private set

    // 内存缓存：当前学生ID (新增，以便 ViewModel 同步获取)
    @Volatile
    var cachedStudentId: String? = null
        private set

    /**
     * 读取 Token Flow
     */
    val tokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val token = preferences[TOKEN_KEY]
            cachedToken = token // 更新缓存
            token
        }

    /**
     * 读取当前选中的学号 Flow
     */
    val currentStudentId: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val id = preferences[CURRENT_STUDENT_ID_KEY]
            cachedStudentId = id // 更新缓存
            id
        }

    /**
     * 保存 Token
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * 保存当前选中的学号
     */
    suspend fun saveCurrentStudentId(studentId: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STUDENT_ID_KEY] = studentId
        }
    }

    /**
     * 清除 Token (修复：PersonRepository 需要调用此方法)
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        cachedToken = null
        cachedStudentId = null
    }

    /**
     * 同步获取 Token (修复：AuthInterceptor 需要调用此方法)
     */
    suspend fun getTokenSynchronously(): String? {
        // first() 会挂起直到 DataStore 读取完成
        return tokenFlow.first()
    }


    /**
     * 读取用户专业信息 (jg_id, zyh_id, njdm_id)
     */
    val userInfoFlow: Flow<Map<String, String?>> = context.dataStore.data
        .map { prefs ->
            mapOf(
                "jg_id" to prefs[JG_ID_KEY],
                "zyh_id" to prefs[ZYH_ID_KEY],
                "njdm_id" to prefs[NJDM_ID_KEY]
            )
        }

    /**
     * 保存用户专业信息
     */
    suspend fun saveUserInfo(jgId: String, zyhId: String, njdmId: String) {
        context.dataStore.edit { prefs ->
            if (jgId.isNotEmpty()) prefs[JG_ID_KEY] = jgId
            if (zyhId.isNotEmpty()) prefs[ZYH_ID_KEY] = zyhId
            if (njdmId.isNotEmpty()) prefs[NJDM_ID_KEY] = njdmId
        }
    }
}