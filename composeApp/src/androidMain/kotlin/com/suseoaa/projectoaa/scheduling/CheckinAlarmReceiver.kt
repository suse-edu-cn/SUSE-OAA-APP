package com.suseoaa.projectoaa.scheduling

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.presentation.checkin.CheckinExecutor
import com.suseoaa.projectoaa.presentation.checkin.CheckinTimeCalculator
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.di.getSharedModules
import com.suseoaa.projectoaa.shared.util.OaaClock
import com.suseoaa.projectoaa.util.CaptchaOcrRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class CheckinAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SUSEOAA:CheckinAlarm")
            .apply { acquire(10 * 60 * 1000L) }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureKoinInitialized(context)
                CaptchaOcrRecognizer.initialize(context.applicationContext)

                val koin = GlobalContext.get()
                val manager = koin.get<ScheduledCheckinManager>()
                val config = manager.getConfig()

                if (!config.enabled || config.targetAccountIds.isEmpty()) {
                    println("[CheckinAlarmReceiver] 未启用或无目标账号")
                    return@launch
                }

                if (manager.hasAlreadyRunToday(config)) {
                    println("[CheckinAlarmReceiver] 今日已执行过签到，跳过")
                    return@launch
                }

                val repository = koin.get<CheckinRepository>()
                val executor = koin.get<CheckinExecutor>()

                val allAccounts = repository.getAllAccounts()
                val targetAccounts = allAccounts.filter { account ->
                    !account.isQrCodeLogin && account.id in config.targetAccountIds
                }

                if (targetAccounts.isEmpty()) {
                    println("[CheckinAlarmReceiver] 没有可用的密码登录账号")
                    manager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), "没有可用的密码登录账号")
                    return@launch
                }

                println("[CheckinAlarmReceiver] 开始执行签到，${targetAccounts.size} 个账号")
                val result = executor.executeForAccounts(
                    accounts = targetAccounts,
                    maxRetryCount = config.maxRetryCount,
                    retryIntervalMinutes = config.retryIntervalMinutes
                )

                println("[CheckinAlarmReceiver] ${result.summary}")
                manager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), result.summary)

                val today = OaaClock.now()
                    .toLocalDateTime(TimeZone.of("Asia/Shanghai")).date.toString()
                manager.updateLastRunDate(today)

                // 重新调度下一天的闹钟
                val freshConfig = manager.getConfig()
                CheckinAlarmManager.scheduleNextAlarm(context, freshConfig)

            } catch (e: Exception) {
                println("[CheckinAlarmReceiver] 执行异常: ${e.message}")
            } finally {
                wakeLock.release()
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
