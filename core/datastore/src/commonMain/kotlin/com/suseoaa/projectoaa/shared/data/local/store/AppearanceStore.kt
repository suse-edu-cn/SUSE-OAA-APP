package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 可单独设置背景图的页面。 */
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
 * 外观：动态取色与各页面背景图。
 */
class AppearanceStore(private val dataStore: DataStore<Preferences>) {

    val dynamicColorEnabledFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.DYNAMIC_COLOR_ENABLED] ?: false }

    // 亮/暗两套取色是后加的；读不到时回落到早期版本写入的单色键，保证老用户升级后配色不变。
    val dynamicColorPaletteLightFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR_PALETTE_LIGHT] ?: prefs[Keys.DYNAMIC_COLOR_PALETTE_LEGACY]
    }

    val dynamicColorPaletteDarkFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR_PALETTE_DARK] ?: prefs[Keys.DYNAMIC_COLOR_PALETTE_LEGACY]
    }

    val appBackgroundImagesFlow: Flow<Map<String, String?>> = dataStore.data.map { prefs ->
        mapOf(
            BackgroundPageIds.DEFAULT to prefs[Keys.BACKGROUND_IMAGE_DEFAULT],
            BackgroundPageIds.HOME to prefs[Keys.BACKGROUND_IMAGE_HOME],
            BackgroundPageIds.COURSE to prefs[Keys.BACKGROUND_IMAGE_COURSE],
            BackgroundPageIds.ACADEMIC to prefs[Keys.BACKGROUND_IMAGE_ACADEMIC],
            BackgroundPageIds.PERSON to prefs[Keys.BACKGROUND_IMAGE_PERSON],
        )
    }

    suspend fun saveDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun saveDynamicColorPalettes(lightColorHex: String?, darkColorHex: String?) {
        dataStore.edit { prefs ->
            prefs.putOrRemove(Keys.DYNAMIC_COLOR_PALETTE_LIGHT, lightColorHex)
            prefs.putOrRemove(Keys.DYNAMIC_COLOR_PALETTE_DARK, darkColorHex)
        }
    }

    /** [pageIds] 为空表示写入"默认背景"。 */
    suspend fun saveBackgroundImageForPages(imageBase64: String, pageIds: Set<String>) {
        if (imageBase64.isBlank()) return
        dataStore.edit { prefs ->
            if (pageIds.isEmpty()) {
                prefs[Keys.BACKGROUND_IMAGE_DEFAULT] = imageBase64
                return@edit
            }
            pageIds.forEach { pageId ->
                backgroundKeyFor(pageId)?.let { prefs[it] = imageBase64 }
            }
        }
    }

    suspend fun clearBackgroundImageForPages(pageIds: Set<String>) {
        dataStore.edit { prefs ->
            if (pageIds.isEmpty()) {
                prefs.remove(Keys.BACKGROUND_IMAGE_DEFAULT)
                return@edit
            }
            pageIds.forEach { pageId ->
                backgroundKeyFor(pageId)?.let { prefs.remove(it) }
            }
        }
    }

    private fun backgroundKeyFor(pageId: String): Preferences.Key<String>? = when (pageId) {
        BackgroundPageIds.DEFAULT -> Keys.BACKGROUND_IMAGE_DEFAULT
        BackgroundPageIds.HOME -> Keys.BACKGROUND_IMAGE_HOME
        BackgroundPageIds.COURSE -> Keys.BACKGROUND_IMAGE_COURSE
        BackgroundPageIds.ACADEMIC -> Keys.BACKGROUND_IMAGE_ACADEMIC
        BackgroundPageIds.PERSON -> Keys.BACKGROUND_IMAGE_PERSON
        else -> null
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value.isNullOrBlank()) remove(key) else set(key, value)
    }

    private object Keys {
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val DYNAMIC_COLOR_PALETTE_LEGACY = stringPreferencesKey("dynamic_color_palette")
        val DYNAMIC_COLOR_PALETTE_LIGHT = stringPreferencesKey("dynamic_color_palette_light")
        val DYNAMIC_COLOR_PALETTE_DARK = stringPreferencesKey("dynamic_color_palette_dark")
        val BACKGROUND_IMAGE_DEFAULT = stringPreferencesKey("background_image_default")
        val BACKGROUND_IMAGE_HOME = stringPreferencesKey("background_image_home")
        val BACKGROUND_IMAGE_COURSE = stringPreferencesKey("background_image_course")
        val BACKGROUND_IMAGE_ACADEMIC = stringPreferencesKey("background_image_academic")
        val BACKGROUND_IMAGE_PERSON = stringPreferencesKey("background_image_person")
    }
}
