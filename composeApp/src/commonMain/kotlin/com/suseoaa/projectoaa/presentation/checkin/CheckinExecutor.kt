package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import kotlinx.coroutines.delay

data class CheckinExecutionResult(
    val successCount: Int,
    val failCount: Int,
    val total: Int,
    val messages: List<String> = emptyList()
) {
    val summary: String get() = "签到完成: 成功 $successCount，失败 $failCount，共 $total 个账号"
}

class CheckinExecutor(
    private val checkinRepository: CheckinRepository,
    private val autoLogin: PasswordAutoLogin
) {
    suspend fun executeForAccounts(
        accounts: List<CheckinAccountData>,
        maxRetryCount: Int,
        retryIntervalMinutes: Int,
        onProgress: ((currentAccount: String, progress: Int, total: Int) -> Unit)? = null
    ): CheckinExecutionResult {
        var successCount = 0
        var failCount = 0
        val total = accounts.size
        val messages = mutableListOf<String>()

        for ((index, account) in accounts.withIndex()) {
            onProgress?.invoke(
                account.name.ifBlank { account.studentId },
                index + 1,
                total
            )

            val (success, message) = executeForAccount(account, maxRetryCount, retryIntervalMinutes)
            if (success) successCount++ else failCount++
            
            // 构造带名字的提示消息
            val accountName = account.name.ifBlank { account.studentId }
            messages.add("[$accountName] $message")

            if (index < accounts.size - 1) {
                delay(1000)
            }
        }

        return CheckinExecutionResult(successCount, failCount, total, messages)
    }

    private suspend fun executeForAccount(
        account: CheckinAccountData,
        maxRetry: Int,
        retryIntervalMinutes: Int
    ): Pair<Boolean, String> {
        var lastMessage = "未知错误"
        repeat(maxRetry + 1) { attempt ->
            if (attempt > 0) {
                println("[CheckinExecutor] 账号 ${account.studentId} 第 $attempt 次重试")
                delay(retryIntervalMinutes * 60_000L)
            }
            val (success, message) = performAutoCheckin(account)
            lastMessage = message
            if (success) return Pair(true, message)
        }
        return Pair(false, lastMessage)
    }

    private suspend fun performAutoCheckin(account: CheckinAccountData): Pair<Boolean, String> {
        return try {
            when (val loginResult = autoLogin.login(account)) {
                is AutoLoginResult.SmsRequired -> {
                    println("[CheckinExecutor] 账号 ${account.studentId} 需要短信验证，跳过")
                    Pair(false, "需要短信验证")
                }

                is AutoLoginResult.Failed -> {
                    println("[CheckinExecutor] 账号 ${account.studentId} 登录失败: ${loginResult.message}")
                    Pair(false, loginResult.message)
                }

                is AutoLoginResult.Success -> when (checkinRepository.performCheckinAfterLogin(account)) {
                    is CheckinResult.Success -> Pair(true, "打卡成功")
                    is CheckinResult.AlreadyChecked -> Pair(true, "已打卡")
                    is CheckinResult.NoTask -> Pair(true, "无任务")
                    is CheckinResult.Failed -> Pair(false, "打卡失败")
                }
            }
        } catch (e: Throwable) {
            println("[CheckinExecutor] 账号 ${account.studentId} 异常: ${e.message}")
            Pair(false, e.message ?: "未知异常")
        }
    }
}
