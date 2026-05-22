package com.suseoaa.projectoaa.presentation.checkin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 定时签到配置
 */
@Serializable
data class SchedulerConfig(
    val enabled: Boolean = false,
    val targetAccountIds: List<Long> = emptyList(),
    val scheduledHour: Int = 7,
    val scheduledMinute: Int = 0,
    val scheduledSecond: Int = 0,
    val maxRetryCount: Int = 3,
    val retryIntervalMinutes: Int = 5,
    val lastRunTimestamp: String? = null,
    val lastRunResult: String? = null,
    val lastRunDate: String? = null
)

/**
 * 定时签到配置持久化管理器
 * 使用 DataStore 存储 JSON 序列化的 SchedulerConfig
 */
class ScheduledCheckinManager(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    val configFlow: Flow<SchedulerConfig> = dataStore.data.map { prefs ->
        prefs[CONFIG_KEY]?.let {
            try { json.decodeFromString(it) } catch (_: Exception) { SchedulerConfig() }
        } ?: SchedulerConfig()
    }

    suspend fun saveConfig(config: SchedulerConfig) {
        dataStore.edit { prefs ->
            prefs[CONFIG_KEY] = json.encodeToString(config)
        }
    }

    suspend fun getConfig(): SchedulerConfig = configFlow.first()

    suspend fun updateLastRun(timestamp: String, result: String) {
        val current = getConfig()
        saveConfig(current.copy(lastRunTimestamp = timestamp, lastRunResult = result))
    }

    suspend fun updateLastRunDate(date: String) {
        val current = getConfig()
        saveConfig(current.copy(lastRunDate = date))
    }

    fun hasAlreadyRunToday(config: SchedulerConfig): Boolean {
        val today = OaaClock.now()
            .toLocalDateTime(TimeZone.of("Asia/Shanghai"))
            .date.toString()
        return config.lastRunDate == today
    }

    companion object {
        private val CONFIG_KEY = stringPreferencesKey("scheduled_checkin_config")
    }
}
