package com.suseoaa.projectoaa.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.*

actual fun platformModule() = module {
    single { CourseDatabaseDriverFactory() }
    
    single {
        PreferenceDataStoreFactory.create(
            produceFile = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null
                )
                val requirePath = requireNotNull(documentDirectory).path + "/app.preferences_pb"
                requirePath.toPath()
            }
        )
    }
}
