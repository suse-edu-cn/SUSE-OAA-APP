package com.suseoaa.projectoaa.shared.data.local

import android.content.Context

/**
 * Android DataStore 工厂
 */
fun createDataStore(context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    return createDataStore {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }
}
