package com.suseoaa.projectoaa

import android.app.Application
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.presentation.checkin.CheckinScheduler
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.scheduling.CheckinAlarmManager
import com.suseoaa.projectoaa.shared.di.getSharedModules
import com.suseoaa.projectoaa.util.AppLifecycleObserver
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class OaaApplication : Application() {
    private var lifecycleObserver: AppLifecycleObserver? = null

    override fun onCreate() {
        super.onCreate()

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
                    println("[OaaApplication] 注册签到闹钟失败: ${e.message}")
                }
            }

            lifecycleObserver = AppLifecycleObserver(this).apply {
                startObserving(
                    onForeground = { scheduler.onAppForeground() },
                    onBackground = { scheduler.onAppBackground() }
                )
            }
        } catch (e: Exception) {
            println("[OaaApplication] 启动定时签到调度器失败: ${e.message}")
        }
    }

    override fun onTerminate() {
        lifecycleObserver?.stopObserving()
        super.onTerminate()
    }
}
