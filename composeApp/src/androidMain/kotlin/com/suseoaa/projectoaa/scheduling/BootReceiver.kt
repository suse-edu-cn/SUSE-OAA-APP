package com.suseoaa.projectoaa.scheduling

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.shared.di.getSharedModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureKoinInitialized(context)
                val koin = GlobalContext.get()
                val manager = koin.get<ScheduledCheckinManager>()
                val config = manager.getConfig()

                if (config.enabled && config.targetAccountIds.isNotEmpty()) {
                    CheckinAlarmManager.scheduleNextAlarm(context, config)
                    println("[BootReceiver] 开机后重新注册签到闹钟")
                }

                // 开机拉起常驻课表提醒服务
                try {
                    val serviceIntent = Intent(context, CourseReminderService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    println("[BootReceiver] 开机拉起课程提醒服务")
                } catch (e: Exception) {
                    println("[BootReceiver] 拉起课程提醒服务失败: ${e.message}")
                }
            } catch (e: Exception) {
                println("[BootReceiver] 重新注册闹钟失败: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureKoinInitialized(context: Context) {
        try {
            GlobalContext.get()
        } catch (_: Exception) {
            startKoin {
                androidContext(context.applicationContext as Application)
                modules(getSharedModules() + listOf(platformModule(), appModule))
            }
        }
    }
}
