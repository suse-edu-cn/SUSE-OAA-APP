package com.suseoaa.projectoaa

import android.app.Application
import android.content.pm.ApplicationInfo
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.domain.checkin.CheckinScheduler
import com.suseoaa.projectoaa.domain.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.scheduling.CheckinAlarmManager
import com.suseoaa.projectoaa.shared.di.getSharedModules
import com.suseoaa.projectoaa.util.AppLifecycleObserver
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level
import com.suseoaa.projectoaa.shared.util.AppLog

class OaaApplication : Application() {
    private var lifecycleObserver: AppLifecycleObserver? = null

    override fun onCreate() {
        super.onCreate()

        // 日志必须先于一切初始化，否则启动阶段的日志会被丢弃。
        // Release 包不装载 Antilog，AppLog 的调用退化为空操作。
        AppLog.init(debug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@OaaApplication)
            modules(
                getSharedModules() + listOf(
                    platformModule(),
                    appModule
                )
            )
        }

        // 启动定时签到调度器
        try {
            val koin = GlobalContext.get()
            val scheduler = koin.get<CheckinScheduler>()
            scheduler.start()

            // 注册系统闹钟，确保后台也能触发
            kotlinx.coroutines.runBlocking {
                try {
                    val manager = koin.get<ScheduledCheckinManager>()
                    val config = manager.getConfig()
                    if (config.enabled && config.targetAccountIds.isNotEmpty()) {
                        CheckinAlarmManager.scheduleNextAlarm(this@OaaApplication, config)
                    }
                } catch (e: Exception) {
                    AppLog.e("[OaaApplication] 注册签到闹钟失败: ${e.message}")
                }
            }

            lifecycleObserver = AppLifecycleObserver(this).apply {
                startObserving(
                    onForeground = { scheduler.onAppForeground() },
                    onBackground = { scheduler.onAppBackground() }
                )
            }
        } catch (e: Exception) {
            AppLog.e("[OaaApplication] 启动定时签到调度器失败: ${e.message}")
        }
    }

    override fun onTerminate() {
        lifecycleObserver?.stopObserving()
        super.onTerminate()
    }
}
