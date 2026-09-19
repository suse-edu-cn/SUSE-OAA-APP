package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 教务系统查询所需的用户档案标识（教工号 / 专业号 / 年级代码）。
 * 绩点、教学计划、学业情况等查询都要带上这三个值。
 */
class UserProfileStore(private val dataStore: DataStore<Preferences>) {

    val userInfoFlow: Flow<Map<String, String?>> = dataStore.data.map { prefs ->
        mapOf(
            "jg_id" to prefs[Keys.JG_ID],
            "zyh_id" to prefs[Keys.ZYH_ID],
            "njdm_id" to prefs[Keys.NJDM_ID]
        )
    }

    /** 空串表示"这次没查到，保留原值"，与改造前行为一致。 */
    suspend fun saveUserInfo(jgId: String, zyhId: String, njdmId: String) {
        dataStore.edit { prefs ->
            if (jgId.isNotEmpty()) prefs[Keys.JG_ID] = jgId
            if (zyhId.isNotEmpty()) prefs[Keys.ZYH_ID] = zyhId
            if (njdmId.isNotEmpty()) prefs[Keys.NJDM_ID] = njdmId
        }
    }

    /** 由 UserDataCleaner 调用；internal 会被模块边界挡住，故为 public。 */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.JG_ID)
            prefs.remove(Keys.ZYH_ID)
            prefs.remove(Keys.NJDM_ID)
        }
    }

    private object Keys {
        val JG_ID = stringPreferencesKey("user_jg_id")
        val ZYH_ID = stringPreferencesKey("user_zyh_id")
        val NJDM_ID = stringPreferencesKey("user_njdm_id")
    }
}
