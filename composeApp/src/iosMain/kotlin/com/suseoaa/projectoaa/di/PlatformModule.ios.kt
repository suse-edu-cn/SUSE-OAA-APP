package com.suseoaa.projectoaa.di

import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.scheduling.PlatformCheckinScheduler
import org.koin.dsl.module
import platform.Foundation.*

actual fun platformModule() = module {
    // App 更新仓库（iOS 实现）
    single {
        val infoDictionary = NSBundle.mainBundle.infoDictionary
        val currentVersion = infoDictionary?.get("CFBundleShortVersionString") as? String ?: "1.0.0"

        AppUpdateRepository(
            httpClient = get(qualifier = org.koin.core.qualifier.named("github")),
            json = get(),
            currentVersionName = currentVersion
        )
    }

    // 定时签到平台调度器
    single { PlatformCheckinScheduler() }
}
