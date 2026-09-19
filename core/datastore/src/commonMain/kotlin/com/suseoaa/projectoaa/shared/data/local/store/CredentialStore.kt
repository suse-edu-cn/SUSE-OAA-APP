package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 教务系统登录口令。
 *
 * 单独成一个 store，是为了让"应用里哪些代码能碰到明文口令"这件事在依赖图上一眼可见：
 * 只有注入了 CredentialStore 的类才拿得到，而不是像以前那样，任何拿到 TokenManager
 * 的地方都顺带获得了读密码的能力。
 *
 * 注意：当前仍以明文写入 DataStore。改为平台 Keystore / Keychain 加密存储需要配套的
 * 数据迁移，属于独立的一次改动。
 */
class CredentialStore(private val dataStore: DataStore<Preferences>) {

    suspend fun savePassword(password: String) {
        dataStore.edit { it[Keys.USER_PASSWORD] = password }
    }

    suspend fun getPasswordSynchronously(): String? =
        dataStore.data.map { it[Keys.USER_PASSWORD] }.first()

    /** 由 UserDataCleaner 调用；internal 会被模块边界挡住，故为 public。 */
    suspend fun clear() {
        dataStore.edit { it.remove(Keys.USER_PASSWORD) }
    }

    private object Keys {
        val USER_PASSWORD = stringPreferencesKey("user_password")
    }
}
