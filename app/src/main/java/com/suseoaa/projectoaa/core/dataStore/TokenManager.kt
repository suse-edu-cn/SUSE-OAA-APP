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
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 1. 定义 DataStore 的扩展属性
// 这里使用 private，确保只有 TokenManager 能直接操作这个具体的 DataStore 文件
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton // 2. 标记为单例：整个 App 只有一份实例，生命周期跟随 App
class TokenManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // 定义 Key (类似 Map 的 Key)
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }
    /**
     * 存Token
     * 使用 TOKEN_KEY 作为索引
     * preferences[TOKEN_KEY] = "eyJhGci..."
     * 取Token
     * 使用 TOKEN_KEY 作为索引
     * val savedToken = preferences[TOKEN_KEY]
     */

    /**
     * 读取 Token
     * 返回的是 Flow，这意味着每当 Token 变化，观察者都会自动收到最新值
     */
    val tokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            // 处理读取时的 IO 异常
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[TOKEN_KEY] // 如果没有值，这里会自动返回 null
        }

    /**
     * 保存 Token
     * suspend 函数，必须在协程中调用
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * 清除 Token (退出登录时调用)
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            // 或者使用 preferences.clear() 清除所有数据
        }
    }
}