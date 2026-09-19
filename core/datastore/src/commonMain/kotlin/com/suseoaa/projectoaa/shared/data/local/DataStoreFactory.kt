package com.suseoaa.projectoaa.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_NAME = "auth_prefs.preferences_pb"

/**
 * 创建全应用共用的 Preferences DataStore。
 *
 * 所有 store（会话、外观、学期、设置……）都读写这同一个文件，拆分的是**代码职责**
 * 而不是存储位置，因此不涉及任何数据迁移，老用户的既有偏好原样可读。
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}
