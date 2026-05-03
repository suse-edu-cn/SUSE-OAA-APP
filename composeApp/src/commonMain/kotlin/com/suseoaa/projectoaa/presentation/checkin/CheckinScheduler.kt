package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed class SchedulerStatus {
    data object Idle : SchedulerStatus()
    data class Scheduled(val nextRunTime: String) : SchedulerStatus()
    data class Running(val currentAccount: String, val progress: Int, val total: Int) : SchedulerStatus()
    data class Completed(val summary: String) : SchedulerStatus()
}

class CheckinScheduler(
    private val scheduledCheckinManager: ScheduledCheckinManager,
    private val checkinRepository: CheckinRepository,
    private val checkinExecutor: CheckinExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var schedulingJob: Job? = null

    private val _status = MutableStateFlow<SchedulerStatus>(SchedulerStatus.Idle)
    val status: StateFlow<SchedulerStatus> = _status.asStateFlow()

    fun start() {
        schedulingJob?.cancel()
        schedulingJob = scope.launch {
            while (isActive) {
                val config = scheduledCheckinManager.getConfig()
                if (!config.enabled || config.targetAccountIds.isEmpty()) {
                    _status.value = SchedulerStatus.Idle
                    return@launch
                }

                val nextRun = CheckinTimeCalculator.calculateNextRunTime(config.scheduledHour, config.scheduledMinute)
                val nextRunStr = CheckinTimeCalculator.formatTime(config.scheduledHour, config.scheduledMinute)
                _status.value = SchedulerStatus.Scheduled("明天 $nextRunStr")

                val now = OaaClock.now()
                val delayMs = nextRun.toEpochMilliseconds() - now.toEpochMilliseconds()
                if (delayMs > 0) {
                    println("[CheckinScheduler] 下次签到: $nextRunStr, ${delayMs / 1000}秒后执行")
                    delay(delayMs)
                }

                val freshConfig = scheduledCheckinManager.getConfig()
                if (!freshConfig.enabled) {
                    _status.value = SchedulerStatus.Idle
                    continue
                }

                executeScheduledCheckin(freshConfig)

                delay(60_000)
            }
        }
    }

    fun stop() {
        schedulingJob?.cancel()
        schedulingJob = null
        _status.value = SchedulerStatus.Idle
    }

    fun onAppForeground() {}

    fun onAppBackground() {}

    private suspend fun executeScheduledCheckin(config: SchedulerConfig) {
        if (scheduledCheckinManager.hasAlreadyRunToday(config)) {
            println("[CheckinScheduler] 今日已执行过签到，跳过")
            return
        }

        val allAccounts = checkinRepository.getAllAccounts()
        val targetAccounts = allAccounts.filter { account ->
            !account.isQrCodeLogin && account.id in config.targetAccountIds
        }

        if (targetAccounts.isEmpty()) {
            val result = "没有可用的密码登录账号"
            println("[CheckinScheduler] $result")
            _status.value = SchedulerStatus.Completed(result)
            scheduledCheckinManager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), result)
            return
        }

        val executionResult = checkinExecutor.executeForAccounts(
            accounts = targetAccounts,
            maxRetryCount = config.maxRetryCount,
            retryIntervalMinutes = config.retryIntervalMinutes,
            onProgress = { currentAccount, progress, total ->
                _status.value = SchedulerStatus.Running(currentAccount, progress, total)
            }
        )

        println("[CheckinScheduler] ${executionResult.summary}")
        _status.value = SchedulerStatus.Completed(executionResult.summary)
        scheduledCheckinManager.updateLastRun(CheckinTimeCalculator.formatCurrentTime(), executionResult.summary)

        val today = OaaClock.now().toLocalDateTime(TimeZone.of("Asia/Shanghai")).date.toString()
        scheduledCheckinManager.updateLastRunDate(today)
    }
}
