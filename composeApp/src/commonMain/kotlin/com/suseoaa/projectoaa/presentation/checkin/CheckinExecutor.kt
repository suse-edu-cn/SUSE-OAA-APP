package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.util.PlatformCaptchaOcr
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
    private val checkinRepository: CheckinRepository
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
            // 1. 尝试 rememberMe 快速登录
            val fastLogin = checkinRepository.tryAutoLoginWithRememberMe(account).getOrDefault(false)
            if (fastLogin) {
                println("[CheckinExecutor] 账号 ${account.studentId} rememberMe 快速登录成功")
                val result = checkinRepository.performCheckinAfterLogin(account)
                return when (result) {
                    is CheckinResult.Success -> Pair(true, "打卡成功")
                    is CheckinResult.AlreadyChecked -> Pair(true, "已打卡")
                    is CheckinResult.NoTask -> Pair(true, "无任务")
                    is CheckinResult.Failed -> Pair(false, "打卡失败")
                }
            }

            // 2. 获取验证码图片
            val captchaResult = checkinRepository.fetchCaptchaImage()
            if (captchaResult.isFailure) {
                val errorMsg = captchaResult.exceptionOrNull()?.message ?: "获取验证码失败"
                println("[CheckinExecutor] $errorMsg")
                return Pair(false, errorMsg)
            }

            val captchaBytes = captchaResult.getOrThrow()

            // 3. OCR 自动识别
            val ocrResult = try {
                PlatformCaptchaOcr.recognize(captchaBytes)
            } catch (t: Throwable) {
                println("[CheckinExecutor] OCR 异常: ${t.message}")
                return Pair(false, "OCR识别异常")
            }
            if (ocrResult.isFailure || ocrResult.getOrNull()?.length != 4) {
                println("[CheckinExecutor] OCR 识别失败")
                return Pair(false, "验证码识别失败")
            }

            val captchaCode = ocrResult.getOrThrow()
            println("[CheckinExecutor] OCR 识别成功: $captchaCode")

            // 4. 登录
            val loginResult = checkinRepository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode,
                accountId = account.id
            )

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "登录失败"
                if (errorMsg.contains("验证码") || errorMsg.contains("captcha", ignoreCase = true)) {
                    println("[CheckinExecutor] 验证码错误，重试")
                    return performAutoCheckin(account)
                }
                if (checkinRepository.isSmsVerificationRequired(loginResult.exceptionOrNull())) {
                    println("[CheckinExecutor] 账号 ${account.studentId} 需要短信验证，跳过")
                    return Pair(false, "需要短信验证")
                }
                println("[CheckinExecutor] 登录失败: $errorMsg")
                return Pair(false, errorMsg)
            }

            // 5. 执行签到
            val checkinResult = checkinRepository.performCheckinAfterLogin(account)
            when (checkinResult) {
                is CheckinResult.Success -> Pair(true, "打卡成功")
                is CheckinResult.AlreadyChecked -> Pair(true, "已打卡")
                is CheckinResult.NoTask -> Pair(true, "无任务")
                is CheckinResult.Failed -> Pair(false, "打卡失败")
            }
        } catch (e: Throwable) {
            println("[CheckinExecutor] 账号 ${account.studentId} 异常: ${e.message}")
            Pair(false, e.message ?: "未知异常")
        }
    }
}
