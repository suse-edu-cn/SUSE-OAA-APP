package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * 调度器运行状态
 */
sealed class SchedulerStatus {
    data object Idle : SchedulerStatus()
    data class Scheduled(val nextRunTime: String) : SchedulerStatus()
    data class Running(val currentAccount: String, val progress: Int, val total: Int) : SchedulerStatus()
    data class Completed(val summary: String) : SchedulerStatus()
}

/**
 * 定时签到调度器
 * 单例，自带 CoroutineScope，不绑定 ViewModel 生命周期
 * 只要 App 进程存活，就会在设定时间执行签到任务
 */
class CheckinScheduler(
    private val scheduledCheckinManager: ScheduledCheckinManager,
    private val checkinRepository: CheckinRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var schedulingJob: Job? = null

    private val _status = MutableStateFlow<SchedulerStatus>(SchedulerStatus.Idle)
    val status: StateFlow<SchedulerStatus> = _status.asStateFlow()

    private var isAppInForeground = true

    /**
     * 启动调度循环
     * 读取配置，计算下次执行时间，delay 到期后执行签到
     */
    fun start() {
        schedulingJob?.cancel()
        schedulingJob = scope.launch {
            while (isActive) {
                val config = scheduledCheckinManager.getConfig()
                if (!config.enabled || config.targetAccountIds.isEmpty()) {
                    _status.value = SchedulerStatus.Idle
                    return@launch
                }

                val nextRun = calculateNextRunTime(config.scheduledHour, config.scheduledMinute)
                val nextRunStr = formatTime(config.scheduledHour, config.scheduledMinute)
                _status.value = SchedulerStatus.Scheduled("明天 $nextRunStr")

                val now = OaaClock.now()
                val delayMs = nextRun.toEpochMilliseconds() - now.toEpochMilliseconds()
                if (delayMs > 0) {
                    println("[CheckinScheduler] 下次签到: $nextRunStr, ${delayMs / 1000}秒后执行")
                    delay(delayMs)
                }

                // 时间到，执行签到
                val freshConfig = scheduledCheckinManager.getConfig()
                if (!freshConfig.enabled) {
                    _status.value = SchedulerStatus.Idle
                    continue
                }

                executeScheduledCheckin(freshConfig)

                // 执行完毕后等待一小段时间，避免同一天内重复触发
                delay(60_000)
            }
        }
    }

    /**
     * 停止调度
     */
    fun stop() {
        schedulingJob?.cancel()
        schedulingJob = null
        _status.value = SchedulerStatus.Idle
    }

    /**
     * App 进入前台
     */
    fun onAppForeground() {
        isAppInForeground = true
    }

    /**
     * App 进入后台
     */
    fun onAppBackground() {
        isAppInForeground = false
    }

    /**
     * 执行定时签到
     */
    private suspend fun executeScheduledCheckin(config: SchedulerConfig) {
        val allAccounts = checkinRepository.getAllAccounts()
        val targetAccounts = allAccounts.filter { account ->
            !account.isQrCodeLogin && account.id in config.targetAccountIds
        }

        if (targetAccounts.isEmpty()) {
            val result = "没有可用的密码登录账号"
            println("[CheckinScheduler] $result")
            _status.value = SchedulerStatus.Completed(result)
            scheduledCheckinManager.updateLastRun(formatCurrentTime(), result)
            return
        }

        var successCount = 0
        var failCount = 0
        val total = targetAccounts.size

        for ((index, account) in targetAccounts.withIndex()) {
            _status.value = SchedulerStatus.Running(
                currentAccount = account.name.ifBlank { account.studentId },
                progress = index + 1,
                total = total
            )

            val accountResult = executeForAccount(account, config.maxRetryCount, config.retryIntervalMinutes)
            if (accountResult) {
                successCount++
            } else {
                failCount++
            }

            // 账号间延迟，避免请求过快
            if (index < targetAccounts.size - 1) {
                delay(1000)
            }
        }

        val summary = "签到完成: 成功 $successCount，失败 $failCount，共 $total 个账号"
        println("[CheckinScheduler] $summary")
        _status.value = SchedulerStatus.Completed(summary)
        scheduledCheckinManager.updateLastRun(formatCurrentTime(), summary)
    }

    /**
     * 为单个账号执行签到，支持重试
     */
    private suspend fun executeForAccount(
        account: CheckinAccountData,
        maxRetry: Int,
        retryIntervalMinutes: Int
    ): Boolean {
        repeat(maxRetry + 1) { attempt ->
            if (attempt > 0) {
                println("[CheckinScheduler] 账号 ${account.studentId} 第 $attempt 次重试")
                delay(retryIntervalMinutes * 60_000L)
            }

            val success = performAutoCheckin(account)
            if (success) return true
        }
        return false
    }

    /**
     * 自动签到流程（复用 CheckinViewModel 的逻辑）
     */
    private suspend fun performAutoCheckin(account: CheckinAccountData): Boolean {
        return try {
            // 1. 尝试 rememberMe 快速登录
            val fastLogin = checkinRepository.tryAutoLoginWithRememberMe(account).getOrDefault(false)
            if (fastLogin) {
                println("[CheckinScheduler] 账号 ${account.studentId} rememberMe 快速登录成功")
                val result = checkinRepository.performCheckinAfterLogin(account)
                return when (result) {
                    is CheckinResult.Success -> { println("[CheckinScheduler] ${account.studentId}: ${result.message}"); true }
                    is CheckinResult.AlreadyChecked -> { println("[CheckinScheduler] ${account.studentId}: ${result.message}"); true }
                    is CheckinResult.NoTask -> { println("[CheckinScheduler] ${account.studentId}: ${result.message}"); true }
                    is CheckinResult.Failed -> { println("[CheckinScheduler] ${account.studentId}: ${result.error}"); false }
                }
            }

            // 2. 获取验证码图片
            val captchaResult = checkinRepository.fetchCaptchaImage()
            if (captchaResult.isFailure) {
                println("[CheckinScheduler] 获取验证码失败: ${captchaResult.exceptionOrNull()?.message}")
                return false
            }

            val captchaBytes = captchaResult.getOrThrow()

            // 3. OCR 自动识别
            val ocrResult = try {
                com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaBytes)
            } catch (t: Throwable) {
                println("[CheckinScheduler] OCR 异常: ${t.message}")
                return false
            }
            if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                println("[CheckinScheduler] OCR 识别失败")
                return false
            }

            val captchaCode = ocrResult.getOrThrow()
            println("[CheckinScheduler] OCR 识别成功: $captchaCode")

            // 4. 登录
            val loginResult = checkinRepository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode,
                accountId = account.id
            )

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: ""
                // 验证码错误可重试
                if (errorMsg.contains("验证码") || errorMsg.contains("captcha", ignoreCase = true)) {
                    println("[CheckinScheduler] 验证码错误，重试")
                    return performAutoCheckin(account) // 递归重试一次
                }
                // 短信验证等无法自动处理的情况
                if (checkinRepository.isSmsVerificationRequired(loginResult.exceptionOrNull())) {
                    println("[CheckinScheduler] 账号 ${account.studentId} 需要短信验证，跳过")
                    return false
                }
                println("[CheckinScheduler] 登录失败: $errorMsg")
                return false
            }

            // 5. 执行签到
            val checkinResult = checkinRepository.performCheckinAfterLogin(account)
            when (checkinResult) {
                is CheckinResult.Success -> { println("[CheckinScheduler] ${account.studentId}: ${checkinResult.message}"); true }
                is CheckinResult.AlreadyChecked -> { println("[CheckinScheduler] ${account.studentId}: ${checkinResult.message}"); true }
                is CheckinResult.NoTask -> { println("[CheckinScheduler] ${account.studentId}: ${checkinResult.message}"); true }
                is CheckinResult.Failed -> { println("[CheckinScheduler] ${account.studentId}: ${checkinResult.error}"); false }
            }
        } catch (e: Throwable) {
            println("[CheckinScheduler] 账号 ${account.studentId} 异常: ${e.message}")
            false
        }
    }

    /**
     * 计算下次执行时间
     * 如果今天该时间已过，则计算明天的
     */
    private fun calculateNextRunTime(hour: Int, minute: Int): kotlinx.datetime.Instant {
        val tz = TimeZone.of("Asia/Shanghai")
        val now = OaaClock.now().toLocalDateTime(tz)

        var target = LocalDateTime(now.date.year, now.date.monthNumber, now.date.dayOfMonth, hour, minute, 0)

        // 如果今天该时间已过，设为明天
        if (target.toInstant(tz) <= now.toInstant(tz)) {
            val tomorrow = now.date.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
            target = LocalDateTime(tomorrow.year, tomorrow.monthNumber, tomorrow.dayOfMonth, hour, minute, 0)
        }

        return target.toInstant(tz)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun formatCurrentTime(): String {
        val now = OaaClock.now().toLocalDateTime(TimeZone.of("Asia/Shanghai"))
        return "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }
}
