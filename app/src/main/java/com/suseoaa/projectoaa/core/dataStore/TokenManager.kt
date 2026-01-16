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
    }

//    内存缓存，使用 @Volatile 保证线程可见性
    @Volatile
    var cachedToken: String? = null
        private set

    /**
     * 读取 Token
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
     * 读取当前选中的学号
     * 返回 Flow，任何地方修改了 ID，这里都会收到通知
     */
    val currentStudentId: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[CURRENT_STUDENT_ID_KEY]
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
     * 清除 Token
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            // 或者只移除特定的 key: preferences.remove(TOKEN_KEY)
            preferences.clear() 
        }
    }

    suspend fun getTokenSynchronously(): String? {
        // first() 会挂起直到 DataStore 读取完成
        return tokenFlow.first()
    }


}