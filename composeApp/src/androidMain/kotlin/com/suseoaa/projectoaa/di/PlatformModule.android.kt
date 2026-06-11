package com.suseoaa.projectoaa.di

import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.scheduling.PlatformCheckinScheduler
import com.suseoaa.projectoaa.widget.AndroidWidgetRefresher
import com.suseoaa.projectoaa.widget.WidgetRefresher
import org.koin.dsl.module

actual fun platformModule() = module {
    // App 更新仓库（Android 特定实现）
    single {
        val context = get<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = packageInfo.versionName ?: "1.0.0"
        
        AppUpdateRepository(
            context = context,
            httpClient = get(qualifier = org.koin.core.qualifier.named("github")),
            json = get(),
            currentVersionName = currentVersion
        )
    }

    // 定时签到平台调度器
    single { PlatformCheckinScheduler(get()) }

    // 桌面小组件刷新器（Android 实现）
    single<WidgetRefresher> { AndroidWidgetRefresher(get()) }
}
