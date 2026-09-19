package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 学期日历配置：开学日期与是否存在第 0 周。
 * 课表排版、上课提醒、桌面小组件都按这两个值折算周次。
 */
class SemesterStore(private val dataStore: DataStore<Preferences>) {

    suspend fun getSemesterStartDate(): String? =
        dataStore.data.map { it[Keys.SEMESTER_START_DATE] }.first()

    suspend fun saveSemesterStartDate(dateString: String) {
        dataStore.edit { it[Keys.SEMESTER_START_DATE] = dateString }
    }

    suspend fun getSemesterHasWeekZero(): Boolean =
        dataStore.data.map { it[Keys.SEMESTER_HAS_WEEK_ZERO] ?: false }.first()

    suspend fun saveSemesterHasWeekZero(hasWeekZero: Boolean) {
        dataStore.edit { it[Keys.SEMESTER_HAS_WEEK_ZERO] = hasWeekZero }
    }

    private object Keys {
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val SEMESTER_HAS_WEEK_ZERO = booleanPreferencesKey("semester_has_week_zero")
    }
}
