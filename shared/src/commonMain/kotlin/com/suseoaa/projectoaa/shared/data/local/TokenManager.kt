package com.suseoaa.projectoaa.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/**
 * DataStore 偏好设置 Keys
 */
object PreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("jwt_token")
    val USER_ID = stringPreferencesKey("user_id")
    val CURRENT_STUDENT_ID = stringPreferencesKey("current_student_id")
    val USER_PASSWORD = stringPreferencesKey("user_password")
    val TOKEN_LAST_UPDATE_TIME = stringPreferencesKey("token_last_update_time")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val KAGGLE_AUTH_TOKEN = stringPreferencesKey("kaggle_auth_token") // Kaggle API Basic Auth Base64
    
    // 用户绩点计算相关
    val JG_ID = stringPreferencesKey("user_jg_id")
    val ZYH_ID = stringPreferencesKey("user_zyh_id")
    val NJDM_ID = stringPreferencesKey("user_njdm_id")
    
    // 设置
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val DYNAMIC_COLOR_PALETTE = stringPreferencesKey("dynamic_color_palette")
    val DYNAMIC_COLOR_PALETTE_LIGHT = stringPreferencesKey("dynamic_color_palette_light")
    val DYNAMIC_COLOR_PALETTE_DARK = stringPreferencesKey("dynamic_color_palette_dark")
    val BACKGROUND_IMAGE_DEFAULT = stringPreferencesKey("background_image_default")
    val BACKGROUND_IMAGE_HOME = stringPreferencesKey("background_image_home")
    val BACKGROUND_IMAGE_COURSE = stringPreferencesKey("background_image_course")
    val BACKGROUND_IMAGE_ACADEMIC = stringPreferencesKey("background_image_academic")
    val BACKGROUND_IMAGE_PERSON = stringPreferencesKey("background_image_person")

    // 更新弹窗版本追踪
    val UPDATE_DIALOG_SHOWN_VERSION = stringPreferencesKey("update_dialog_shown_version")

    // 开学日期
    val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")

    // 是否存在第0周
    val SEMESTER_HAS_WEEK_ZERO = booleanPreferencesKey("semester_has_week_zero")

    // 652签到功能解锁
    val CHECKIN_UNLOCKED = booleanPreferencesKey("checkin_feature_unlocked")

    // 默认起始页（0=首页, 1=课程, 2=教务信息, 3=个人）
    val DEFAULT_START_TAB = intPreferencesKey("default_start_tab")

    // 预测性返回手势
    val PREDICTIVE_BACK_ENABLED = booleanPreferencesKey("predictive_back_enabled")

    // 液态玻璃导航栏
    val LIQUID_GLASS_TABBAR_ENABLED = booleanPreferencesKey("liquid_glass_tabbar_enabled")

    // AiLab 持久化设置
    val AILAB_SELECTED_MODEL_ID = stringPreferencesKey("ailab_selected_model_id")
    val AILAB_PREFER_GPU = booleanPreferencesKey("ailab_prefer_gpu")
}

internal const val DATA_STORE_FILE_NAME = "auth_prefs.preferences_pb"

object BackgroundPageIds {
    const val DEFAULT = "default"
    const val HOME = "home"
    const val COURSE = "course"
    const val ACADEMIC = "academic"
    const val PERSON = "person"

    val mainPages: Set<String> = setOf(HOME, COURSE, ACADEMIC, PERSON)

    val allPages: Set<String> = setOf(DEFAULT, HOME, COURSE, ACADEMIC, PERSON)
}

/**
 * 创建 DataStore
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}

/**
 * Token 管理器 - 跨平台
 */
class TokenManager(private val dataStore: DataStore<Preferences>) {
    
    // 内存缓存
    @kotlin.concurrent.Volatile
    var cachedToken: String? = null
        private set
    
    @kotlin.concurrent.Volatile
    var cachedStudentId: String? = null
        private set

    // ==================== Token & Auth ====================

    val tokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[PreferencesKeys.USER_TOKEN]
        cachedToken = token
        token
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.CURRENT_STUDENT_ID]
        cachedStudentId = id
        id
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TOKEN] = token
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
        }
        cachedToken = token
    }

    suspend fun savePassword(password: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_PASSWORD] = password
        }
    }

    suspend fun getPasswordSynchronously(): String? {
        return dataStore.data.map { it[PreferencesKeys.USER_PASSWORD] }.first()
    }

    suspend fun saveTokenLastUpdateTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN_LAST_UPDATE_TIME] = timestamp.toString()
        }
    }

    suspend fun getTokenLastUpdateTime(): Long {
        return dataStore.data.map { it[PreferencesKeys.TOKEN_LAST_UPDATE_TIME]?.toLongOrNull() ?: 0L }.first()
    }

    suspend fun saveCurrentStudentId(studentId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STUDENT_ID] = studentId
        }
        cachedStudentId = studentId
    }

    suspend fun getTokenSynchronously(): String? {
        return tokenFlow.first()
    }

    // ==================== User GPA IDs ====================

    val userInfoFlow: Flow<Map<String, String?>> = dataStore.data.map { prefs ->
        mapOf(
            "jg_id" to prefs[PreferencesKeys.JG_ID],
            "zyh_id" to prefs[PreferencesKeys.ZYH_ID],
            "njdm_id" to prefs[PreferencesKeys.NJDM_ID]
        )
    }

    fun getUserGpaIds(): Flow<Triple<String?, String?, String?>> = dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.JG_ID],
            prefs[PreferencesKeys.ZYH_ID],
            prefs[PreferencesKeys.NJDM_ID]
        )
    }

    suspend fun saveUserInfo(jgId: String, zyhId: String, njdmId: String) {
        dataStore.edit { prefs ->
            if (jgId.isNotEmpty()) prefs[PreferencesKeys.JG_ID] = jgId
            if (zyhId.isNotEmpty()) prefs[PreferencesKeys.ZYH_ID] = zyhId
            if (njdmId.isNotEmpty()) prefs[PreferencesKeys.NJDM_ID] = njdmId
        }
    }

    /** Alias for [saveUserInfo] */
    suspend fun saveUserGpaIds(jgId: String, zyhId: String, njdmId: String) =
        saveUserInfo(jgId, zyhId, njdmId)

    // ==================== Theme ====================

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val dynamicColorEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: false
    }

    val dynamicColorPaletteFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE]
    }

    val dynamicColorPaletteLightFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_LIGHT]
            ?: preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE]
    }

    val dynamicColorPaletteDarkFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_DARK]
            ?: preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE]
    }

    val appBackgroundImagesFlow: Flow<Map<String, String?>> = dataStore.data.map { preferences ->
        mapBackgroundImages(preferences)
    }

    val defaultBackgroundImageFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BACKGROUND_IMAGE_DEFAULT]
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun saveDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun saveDynamicColorPalette(colorHex: String?) {
        dataStore.edit { preferences ->
            if (colorHex.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.DYNAMIC_COLOR_PALETTE)
            } else {
                preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE] = colorHex
            }
        }
    }

    suspend fun saveDynamicColorPaletteLight(colorHex: String?) {
        dataStore.edit { preferences ->
            if (colorHex.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.DYNAMIC_COLOR_PALETTE_LIGHT)
            } else {
                preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_LIGHT] = colorHex
            }
        }
    }

    suspend fun saveDynamicColorPaletteDark(colorHex: String?) {
        dataStore.edit { preferences ->
            if (colorHex.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.DYNAMIC_COLOR_PALETTE_DARK)
            } else {
                preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_DARK] = colorHex
            }
        }
    }

    suspend fun saveDynamicColorPalettes(lightColorHex: String?, darkColorHex: String?) {
        dataStore.edit { preferences ->
            if (lightColorHex.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.DYNAMIC_COLOR_PALETTE_LIGHT)
            } else {
                preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_LIGHT] = lightColorHex
            }

            if (darkColorHex.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.DYNAMIC_COLOR_PALETTE_DARK)
            } else {
                preferences[PreferencesKeys.DYNAMIC_COLOR_PALETTE_DARK] = darkColorHex
            }
        }
    }

    suspend fun saveBackgroundImageForPages(imageBase64: String, pageIds: Set<String>) {
        if (imageBase64.isBlank()) return

        dataStore.edit { preferences ->
            if (pageIds.isEmpty()) {
                preferences[PreferencesKeys.BACKGROUND_IMAGE_DEFAULT] = imageBase64
                return@edit
            }

            pageIds.forEach { pageId ->
                backgroundImagePreferenceKey(pageId)?.let { key ->
                    preferences[key] = imageBase64
                }
            }
        }
    }

    suspend fun clearDefaultBackgroundImage() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.BACKGROUND_IMAGE_DEFAULT)
        }
    }

    suspend fun clearBackgroundImageForPages(pageIds: Set<String>) {
        dataStore.edit { preferences ->
            if (pageIds.isEmpty()) {
                preferences.remove(PreferencesKeys.BACKGROUND_IMAGE_DEFAULT)
                return@edit
            }

            pageIds.forEach { pageId ->
                backgroundImagePreferenceKey(pageId)?.let { key ->
                    preferences.remove(key)
                }
            }
        }
    }

    private fun mapBackgroundImages(preferences: Preferences): Map<String, String?> = mapOf(
        BackgroundPageIds.DEFAULT to preferences[PreferencesKeys.BACKGROUND_IMAGE_DEFAULT],
        BackgroundPageIds.HOME to preferences[PreferencesKeys.BACKGROUND_IMAGE_HOME],
        BackgroundPageIds.COURSE to preferences[PreferencesKeys.BACKGROUND_IMAGE_COURSE],
        BackgroundPageIds.ACADEMIC to preferences[PreferencesKeys.BACKGROUND_IMAGE_ACADEMIC],
        BackgroundPageIds.PERSON to preferences[PreferencesKeys.BACKGROUND_IMAGE_PERSON],
    )

    private fun backgroundImagePreferenceKey(pageId: String): Preferences.Key<String>? {
        return when (pageId) {
            BackgroundPageIds.DEFAULT -> PreferencesKeys.BACKGROUND_IMAGE_DEFAULT
            BackgroundPageIds.HOME -> PreferencesKeys.BACKGROUND_IMAGE_HOME
            BackgroundPageIds.COURSE -> PreferencesKeys.BACKGROUND_IMAGE_COURSE
            BackgroundPageIds.ACADEMIC -> PreferencesKeys.BACKGROUND_IMAGE_ACADEMIC
            BackgroundPageIds.PERSON -> PreferencesKeys.BACKGROUND_IMAGE_PERSON
            else -> null
        }
    }

    // ==================== 更新弹窗状态管理 ====================

    val updateDialogShownVersionFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.UPDATE_DIALOG_SHOWN_VERSION]
    }

    suspend fun hasShownUpdateDialogForVersion(version: String): Boolean {
        val shownVersion = updateDialogShownVersionFlow.first()
        return shownVersion == version
    }

    suspend fun markUpdateDialogShown(version: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.UPDATE_DIALOG_SHOWN_VERSION] = version
        }
    }

    // ==================== 开学日期管理 ====================

    val semesterStartDateFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SEMESTER_START_DATE]
    }

    suspend fun saveSemesterStartDate(dateString: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEMESTER_START_DATE] = dateString
        }
    }

    suspend fun getSemesterStartDate(): String? {
        return semesterStartDateFlow.first()
    }

    val semesterHasWeekZeroFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SEMESTER_HAS_WEEK_ZERO] ?: false
    }

    suspend fun saveSemesterHasWeekZero(hasWeekZero: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEMESTER_HAS_WEEK_ZERO] = hasWeekZero
        }
    }

    suspend fun getSemesterHasWeekZero(): Boolean {
        return semesterHasWeekZeroFlow.first()
    }

    // ==================== 652签到功能解锁状态 ====================

    val checkinUnlockedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CHECKIN_UNLOCKED] ?: false
    }

    suspend fun unlockCheckinFeature() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CHECKIN_UNLOCKED] = true
        }
    }

    suspend fun isCheckinUnlocked(): Boolean {
        return checkinUnlockedFlow.first()
    }

    // ==================== 默认起始页设置 ====================

    val defaultStartTabFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_START_TAB] ?: 0
    }

    suspend fun saveDefaultStartTab(tabIndex: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_START_TAB] = tabIndex
        }
    }

    // ==================== 预测性返回手势设置 ====================

    val predictiveBackEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.PREDICTIVE_BACK_ENABLED] ?: true
    }

    suspend fun savePredictiveBackEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PREDICTIVE_BACK_ENABLED] = enabled
        }
    }

    // ==================== 液态玻璃导航栏设置 ====================

    val liquidGlassTabbarEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LIQUID_GLASS_TABBAR_ENABLED] ?: false
    }

    suspend fun saveLiquidGlassTabbarEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LIQUID_GLASS_TABBAR_ENABLED] = enabled
        }
    }

    // ==================== 清除数据 ====================

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_TOKEN)
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
        }
        cachedToken = null
    }

    /** 清除用户会话相关数据，保留系统设置 */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_TOKEN)
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.CURRENT_STUDENT_ID)
            preferences.remove(PreferencesKeys.USER_PASSWORD)
            preferences.remove(PreferencesKeys.TOKEN_LAST_UPDATE_TIME)
            preferences.remove(PreferencesKeys.JG_ID)
            preferences.remove(PreferencesKeys.ZYH_ID)
            preferences.remove(PreferencesKeys.NJDM_ID)
            // 不清除 THEME_MODE, BACKGROUND_IMAGE 等全局设置
        }
        cachedToken = null
        cachedStudentId = null
        
        // 清理所有的 HttpClient Cookie 缓存
        com.suseoaa.projectoaa.shared.data.remote.network.SessionCleaner.clearAllNetworkSessions()
    }

    /** 彻底清除所有数据 (仅在真的需要恢复出厂设置时使用) */
    suspend fun clear() {
        clearSession() // 默认调用 clearSession 避免误删主题等设置
    }

    /** Alias for [clear] */
    suspend fun clearAll() = clear()
    // ==================== Kaggle Auth 相关 ====================

    /**
     * 获取 Kaggle Auth 流
     */
    val kaggleAuthFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KAGGLE_AUTH_TOKEN]
    }

    // 获取/设置选中的模型ID
    val aiLabSelectedModelIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AILAB_SELECTED_MODEL_ID]
    }
    suspend fun saveAiLabSelectedModelId(modelId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AILAB_SELECTED_MODEL_ID] = modelId
        }
    }

    // 获取/设置是否偏好GPU
    val aiLabPreferGpuFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AILAB_PREFER_GPU] ?: true
    }
    suspend fun saveAiLabPreferGpu(preferGpu: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AILAB_PREFER_GPU] = preferGpu
        }
    }

    /**
     * 保存 Kaggle Auth
     */
    suspend fun saveKaggleAuth(authBase64: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KAGGLE_AUTH_TOKEN] = authBase64
        }
    }

    /**
     * 获取模型 ETag 流
     */
    fun getModelETagFlow(modelId: String): Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey("etag_$modelId")]
        }

    /**
     * 保存模型 ETag
     */
    suspend fun saveModelETag(modelId: String, etag: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("etag_$modelId")] = etag
        }
    }
}
