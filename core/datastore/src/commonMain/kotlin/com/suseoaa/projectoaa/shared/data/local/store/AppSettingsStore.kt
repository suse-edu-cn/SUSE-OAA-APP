package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 应用级用户偏好：起始页、手势、导航栏样式、隐藏功能解锁、更新弹窗记录、列表排序。
 * 都不随退出登录清除。
 */
class AppSettingsStore(private val dataStore: DataStore<Preferences>) {

    // ---------- 隐藏的签到功能解锁 ----------
    val checkinUnlockedFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.CHECKIN_UNLOCKED] ?: false }

    suspend fun isCheckinUnlocked(): Boolean = checkinUnlockedFlow.first()

    suspend fun unlockCheckinFeature() {
        dataStore.edit { it[Keys.CHECKIN_UNLOCKED] = true }
    }

    // ---------- 默认起始页 ----------
    val defaultStartTabFlow: Flow<Int> = dataStore.data.map { it[Keys.DEFAULT_START_TAB] ?: 0 }

    suspend fun saveDefaultStartTab(tabIndex: Int) {
        dataStore.edit { it[Keys.DEFAULT_START_TAB] = tabIndex }
    }

    // ---------- 预测式返回手势 ----------
    val predictiveBackEnabledFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.PREDICTIVE_BACK_ENABLED] ?: true }

    suspend fun savePredictiveBackEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PREDICTIVE_BACK_ENABLED] = enabled }
    }

    // ---------- 液态玻璃底栏 ----------
    val liquidGlassTabbarEnabledFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.LIQUID_GLASS_TABBAR_ENABLED] ?: false }

    suspend fun saveLiquidGlassTabbarEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LIQUID_GLASS_TABBAR_ENABLED] = enabled }
    }

    val liquidGlassTabbarStyleFlow: Flow<Int> =
        dataStore.data.map { it[Keys.LIQUID_GLASS_TABBAR_STYLE] ?: 1 }

    suspend fun saveLiquidGlassTabbarStyle(style: Int) {
        dataStore.edit { it[Keys.LIQUID_GLASS_TABBAR_STYLE] = style }
    }

    // ---------- 更新弹窗去重 ----------
    suspend fun hasShownUpdateDialogForVersion(version: String): Boolean =
        dataStore.data.map { it[Keys.UPDATE_DIALOG_SHOWN_VERSION] }.first() == version

    suspend fun markUpdateDialogShown(version: String) {
        dataStore.edit { it[Keys.UPDATE_DIALOG_SHOWN_VERSION] = version }
    }

    // ---------- 物品价值列表排序 ----------
    val assetSortTypeFlow: Flow<String?> = dataStore.data.map { it[Keys.ASSET_SORT_TYPE] }

    suspend fun saveAssetSortType(sortType: String) {
        dataStore.edit { it[Keys.ASSET_SORT_TYPE] = sortType }
    }

    private object Keys {
        val CHECKIN_UNLOCKED = booleanPreferencesKey("checkin_feature_unlocked")
        val DEFAULT_START_TAB = intPreferencesKey("default_start_tab")
        val PREDICTIVE_BACK_ENABLED = booleanPreferencesKey("predictive_back_enabled")
        val LIQUID_GLASS_TABBAR_ENABLED = booleanPreferencesKey("liquid_glass_tabbar_enabled")
        val LIQUID_GLASS_TABBAR_STYLE = intPreferencesKey("liquid_glass_tabbar_style")
        val UPDATE_DIALOG_SHOWN_VERSION = stringPreferencesKey("update_dialog_shown_version")
        val ASSET_SORT_TYPE = stringPreferencesKey("asset_sort_type")
    }
}
