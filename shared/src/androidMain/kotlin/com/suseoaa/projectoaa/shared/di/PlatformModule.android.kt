package com.suseoaa.projectoaa.shared.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.local.createDataStore
import com.suseoaa.projectoaa.shared.data.local.database.CourseDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // DataStore - 需要 Android 的 Context
    single<DataStore<Preferences>> { createDataStore(get<Context>()) }
    single { TokenManager(get<DataStore<Preferences>>()) }
    
    // 数据库 - 需要 Android 的 Context
    single { CourseDatabaseDriverFactory(get<Context>()) }
}
