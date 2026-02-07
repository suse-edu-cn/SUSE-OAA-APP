package com.suseoaa.projectoaa.shared.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.local.createDataStore
import com.suseoaa.projectoaa.shared.data.local.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // DataStore
    single<DataStore<Preferences>> { createDataStore() }
    single { TokenManager(get<DataStore<Preferences>>()) }

    // Database
    single { DatabaseDriverFactory() }
}
