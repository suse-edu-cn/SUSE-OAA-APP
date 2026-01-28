package com.suseoaa.projectoaa.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.*

actual fun platformModule() = module {
    single { CourseDatabaseDriverFactory() }
    
    single {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null
                )
                val requirePath = requireNotNull(documentDirectory).path + "/auth_prefs.preferences_pb"
                requirePath.toPath()
            }
        )
    }
    
    // App 更新仓库（iOS 实现）
    single {
        // iOS 端获取版本号
        val infoDictionary = NSBundle.mainBundle.infoDictionary
        val currentVersion = infoDictionary?.get("CFBundleShortVersionString") as? String ?: "1.0.0"
        
        AppUpdateRepository(
            httpClient = get(qualifier = org.koin.core.qualifier.named("oaa")),
            json = get(),
            currentVersionName = currentVersion
        )
    }
}
