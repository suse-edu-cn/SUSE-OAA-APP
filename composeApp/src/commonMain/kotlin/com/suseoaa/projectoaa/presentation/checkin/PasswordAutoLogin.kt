package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.util.PlatformCaptchaOcr

/** 自动登录的结果；短信二次验证需要用户参与，因此单独成一档 */
sealed interface AutoLoginResult {
    data object Success : AutoLoginResult

    /** 服务端要求短信二次验证，自动流程无法继续，需要用户手动输入验证码 */
    data object SmsRequired : AutoLoginResult

    data class Failed(val message: String) : AutoLoginResult
}

/**
 * 密码账号的自动登录。
 *
 * 流程：先复用 rememberMe 登录态，未命中再走「取图形验证码 → OCR 识别 → 提交登录」，
 * OCR 或验证码出错时最多重试 [MAX_RETRY] 次。
 *
 * 前台的批量/单账号打卡与后台定时打卡此前各自实现了一份，其中后台那份在验证码
 * 识别错误时会无限递归重试，统一到这里后重试次数由 [MAX_RETRY] 统一约束。
 */
class PasswordAutoLogin(private val repository: CheckinRepository) {

    suspend fun login(account: CheckinAccountData): AutoLoginResult = attempt(account, retryCount = 0)

    private suspend fun attempt(account: CheckinAccountData, retryCount: Int): AutoLoginResult {
        return try {
            // 只在首次尝试时走快速登录，重试说明这条路已经不通了
            if (retryCount == 0) {
                val fastLogin = repository.tryAutoLoginWithRememberMe(account).getOrDefault(false)
                if (fastLogin) {
                    println("[AutoLogin] 使用 rememberMe 快速登录成功")
                    return AutoLoginResult.Success
                }
                println("[AutoLogin] rememberMe 快速登录未命中，回退验证码登录")
            }

            val captchaResult = repository.fetchCaptchaImage()
            if (captchaResult.isFailure) {
                val message = captchaResult.exceptionOrNull()?.message ?: "获取验证码失败"
                println("[AutoLogin] 获取验证码失败: $message")
                return AutoLoginResult.Failed(message)
            }

            val captchaCode = recognizeCaptcha(captchaResult.getOrThrow())
                ?: return retryOrFail(account, retryCount, "验证码识别失败")

            println("[AutoLogin] OCR识别成功: $captchaCode")

            val loginResult = repository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode,
                accountId = account.id
            )

            if (loginResult.isSuccess) {
                println("[AutoLogin] 登录成功")
                return AutoLoginResult.Success
            }

            val error = loginResult.exceptionOrNull()
            if (repository.isSmsVerificationRequired(error)) {
                println("[AutoLogin] 检测到短信二次验证，自动流程无法继续")
                return AutoLoginResult.SmsRequired
            }

            val message = error?.message ?: "登录失败"
            if (isCaptchaError(message)) {
                return retryOrFail(account, retryCount, message)
            }
            println("[AutoLogin] 登录失败: $message")
            AutoLoginResult.Failed(message)
        } catch (e: Throwable) {
            println("[AutoLogin] 异常: ${e.message}")
            AutoLoginResult.Failed(e.message ?: "未知异常")
        }
    }

    /** @return 识别出的 4 位验证码；识别失败或位数不对时返回 null */
    private suspend fun recognizeCaptcha(captchaBytes: ByteArray): String? {
        val ocrResult = try {
            PlatformCaptchaOcr.recognize(captchaBytes)
        } catch (t: Throwable) {
            println("[AutoLogin] OCR 运行时异常: ${t.message}")
            return null
        }
        val code = ocrResult.getOrNull()
        if (ocrResult.isFailure || code?.length != CAPTCHA_LENGTH) {
            println("[AutoLogin] OCR识别失败")
            return null
        }
        return code
    }

    private suspend fun retryOrFail(
        account: CheckinAccountData,
        retryCount: Int,
        message: String
    ): AutoLoginResult {
        if (retryCount >= MAX_RETRY) return AutoLoginResult.Failed(message)
        println("[AutoLogin] $message，重试第 ${retryCount + 1} 次")
        return attempt(account, retryCount + 1)
    }

    private fun isCaptchaError(message: String): Boolean =
        message.contains("验证码") || message.contains("captcha", ignoreCase = true)

    private companion object {
        const val MAX_RETRY = 2
        const val CAPTCHA_LENGTH = 4
    }
}
