package com.suseoaa.projectoaa.scheduling

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
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

        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureKoinInitialized(appContext)
                CaptchaOcrRecognizer.initialize(appContext)

                val koin = GlobalContext.get()
                val manager = koin.get<ScheduledCheckinManager>()
                val config = manager.getConfig()

                println("[CheckinAlarmReceiver] 收到闹钟, enabled=${config.enabled}, accounts=${config.targetAccountIds}")

                if (!config.enabled || config.targetAccountIds.isEmpty()) {
                    println("[CheckinAlarmReceiver] 未启用或无目标账号")
                    showToastOnMainThread(appContext, "652自动签到: 未启用或无目标账号")
                    return@launch
                }

                if (manager.hasAlreadyRunToday(config)) {
                    println("[CheckinAlarmReceiver] 今日已执行过签到，跳过")
                    showToastOnMainThread(appContext, "652自动签到: 今日已执行过，跳过")
                    return@launch
                }

                val repository = koin.get<CheckinRepository>()
                val executor = koin.get<CheckinExecutor>()

                val allAccounts = repository.getAllAccounts()
                val targetAccounts = allAccounts.filter { account ->
                    !account.isQrCodeLogin && account.id in config.targetAccountIds
                }

                println("[CheckinAlarmReceiver] 找到 ${allAccounts.size} 个账号, 目标 ${targetAccounts.size} 个")

                if (targetAccounts.isEmpty()) {
                    println("[CheckinAlarmReceiver] 没有可用的密码登录账号")
                    manager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), "没有可用的密码登录账号")
                    showToastOnMainThread(appContext, "652自动签到: 无可用账号（需密码登录）")
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

                // 逐条显示每个账号的签到结果（格式：xxx 打卡成功/失败/已打卡）
                for (msg in result.messages) {
                    showToastOnMainThread(appContext, msg)
                    kotlinx.coroutines.delay(2000L)
                }

                // 重新调度下一天的闹钟
                val freshConfig = manager.getConfig()
                CheckinAlarmManager.scheduleNextAlarm(appContext, freshConfig)

            } catch (e: Exception) {
                println("[CheckinAlarmReceiver] 执行异常: ${e.message}")
                e.printStackTrace()
                showToastOnMainThread(appContext, "652自动签到异常: ${e.message}")
            } finally {
                wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private fun ensureKoinInitialized(context: Context) {
        try {
            GlobalContext.get()
            println("[CheckinAlarmReceiver] Koin 已初始化")
        } catch (_: Exception) {
            println("[CheckinAlarmReceiver] Koin 未初始化，重新初始化")
            startKoin {
                androidContext(context as Application)
                modules(getSharedModules() + listOf(platformModule(), appModule))
            }
        }
    }

    /**
     * 在主线程弹出原生 Toast，适用于后台 BroadcastReceiver 场景
     * 不依赖 Compose UI，不依赖 ToastManager SharedFlow
     */
    private fun showToastOnMainThread(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
