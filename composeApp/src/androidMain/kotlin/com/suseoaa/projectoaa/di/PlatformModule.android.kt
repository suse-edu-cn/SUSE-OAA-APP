package com.suseoaa.projectoaa.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.suseoaa.projectoaa.data.database.CourseDatabaseDriverFactory
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import okio.Path.Companion.toPath
import org.koin.dsl.module

actual fun platformModule() = module {
    single { CourseDatabaseDriverFactory(get()) }
    
    single {
        val context = get<android.content.Context>()
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve("auth_prefs.preferences_pb").absolutePath.toPath()
            }
        )
    }
    
    // App 更新仓库（Android 特定实现）
    single {
        val context = get<android.content.Context>()
        // 获取当前版本号
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = packageInfo.versionName ?: "1.0.0"
        
        AppUpdateRepository(
            context = context,
            httpClient = get(qualifier = org.koin.core.qualifier.named("github")),
            json = get(),
            currentVersionName = currentVersion
        )
    }
}
