package com.suseoaa.projectoaa.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import okio.Path.Companion.toPath
import org.koin.dsl.module

actual fun platformModule() = module {
    single { CourseDatabaseDriverFactory(get()) }
    
    single {
        val context = get<android.content.Context>()
        PreferenceDataStoreFactory.create(
            produceFile = {
                context.filesDir.resolve("app.preferences_pb")
            }
        )
    }
}
