package com.suseoaa.projectoaa.core.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }

    // 内存缓存
    @Volatile
    var cachedToken: String? = null
        private set

    @Volatile
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
}

