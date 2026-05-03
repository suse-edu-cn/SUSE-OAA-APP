package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.presentation.checkin.CheckinExecutor
import com.suseoaa.projectoaa.presentation.checkin.CheckinTimeCalculator
import com.suseoaa.projectoaa.presentation.checkin.SchedulerConfig
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform

@OptIn(DelicateCoroutinesApi::class)
fun executeBackgroundCheckin(onComplete: (Boolean) -> Unit) {
    GlobalScope.launch(Dispatchers.Default) {
        try {
            initializeKoinIfNeeded()
            val koin = KoinPlatform.getKoin()
            val manager = koin.get<ScheduledCheckinManager>()
            val config = manager.getConfig()

            if (!config.enabled || config.targetAccountIds.isEmpty()) {
                println("[iOS Background] 未启用或无目标账号")
                onComplete(false)
                return@launch
            }

            if (manager.hasAlreadyRunToday(config)) {
                println("[iOS Background] 今日已执行过签到，跳过")
                onComplete(true)
                return@launch
            }

            val repository = koin.get<CheckinRepository>()
            val executor = koin.get<CheckinExecutor>()

            val allAccounts = repository.getAllAccounts()
            val targetAccounts = allAccounts.filter { account ->
                !account.isQrCodeLogin && account.id in config.targetAccountIds
            }

            if (targetAccounts.isEmpty()) {
                println("[iOS Background] 没有可用的密码登录账号")
                manager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), "没有可用的密码登录账号")
                onComplete(false)
                return@launch
            }

            println("[iOS Background] 开始执行签到，${targetAccounts.size} 个账号")
            val result = executor.executeForAccounts(
                accounts = targetAccounts,
                maxRetryCount = config.maxRetryCount,
                retryIntervalMinutes = config.retryIntervalMinutes
            )

            println("[iOS Background] ${result.summary}")
            manager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), result.summary)

            val today = OaaClock.now()
                .toLocalDateTime(TimeZone.of("Asia/Shanghai")).date.toString()
            manager.updateLastRunDate(today)

            onComplete(true)
        } catch (e: Exception) {
            println("[iOS Background] 签到异常: ${e.message}")
            onComplete(false)
        }
    }
}

fun getConfigSync(): SchedulerConfig = runBlocking {
    initializeKoinIfNeeded()
    KoinPlatform.getKoin().get<ScheduledCheckinManager>().getConfig()
}
