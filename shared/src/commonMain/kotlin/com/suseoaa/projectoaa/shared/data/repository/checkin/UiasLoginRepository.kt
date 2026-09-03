package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.data.remote.api.CheckinApiService
import com.suseoaa.projectoaa.shared.data.remote.network.ClearableCookieStorage
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.util.CheckinRSAEncryptor
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * UIAS 统一认证的密码登录链路。
 *
 * 三种进入方式，最终都以拿到 qfhy 的 SESSION Cookie 为准：
 * 1. rememberMe 快速登录 —— 用已保存的 CASTGC 直接换会话，无需再输密码
 * 2. 账号密码 + 图形验证码登录
 * 3. 上一步触发短信二次验证时，补充短信验证码完成登录
 */
class UiasLoginRepository(
    private val api: CheckinApiService,
    private val cookieStorage: ClearableCookieStorage,
    private val accountStore: CheckinAccountStore
) {

    /** 登录页的 execution token，获取验证码时缓存，提交登录时使用 */
    private var cachedExecution: String? = null

    /** 短信二次验证的上下文，供后续发送/提交验证码 */
    private var pendingSmsChallenge: PendingSmsChallenge? = null

    private data class PendingSmsChallenge(
        val username: String,
        val execution: String,
        val phoneMasked: String?,
        val accountId: Long?
    )

    // ==================== rememberMe 快速登录 ====================

    /**
     * 尝试用已保存的 rememberMe(CASTGC) 登录态换取 qfhy SESSION。
     * 成功时无需再走账号密码登录。返回 false 表示不适用或未命中，调用方应退回到密码登录。
     */
    suspend fun tryAutoLoginWithRememberMe(account: CheckinAccountData): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (account.isQrCodeLogin) return@withContext Result.success(false)

                val rememberCookies = account.sessionToken
                if (rememberCookies.isNullOrBlank() || !account.isSessionValid()) {
                    return@withContext Result.success(false)
                }
                if (!rememberCookies.contains("CASTGC=")) {
                    return@withContext Result.success(false)
                }

                // 切换账号前清空旧 Cookie，避免跨账号污染
                cookieStorage.clear()

                val loginPageResponse = api.getLoginPage(rememberCookies)
                val redirectUrl = loginPageResponse.headers[HttpHeaders.Location]
                if (loginPageResponse.status.value !in 300..399 || redirectUrl.isNullOrBlank()) {
                    println(
                        "[Checkin] rememberMe 快速登录未命中重定向，status=${loginPageResponse.status.value}"
                    )
                    return@withContext Result.success(false)
                }

                api.followRedirect(redirectUrl)

                if (!hasQfhySessionCookie()) {
                    println("[Checkin] rememberMe 快速登录后未拿到 qfhy SESSION")
                    return@withContext Result.success(false)
                }

                persistRememberMeCookies(account.id)
                println("[Checkin] rememberMe 快速登录成功: ${account.studentId}")
                Result.success(true)
            } catch (e: Exception) {
                println("[Checkin] rememberMe 快速登录异常: ${e.message}")
                Result.success(false)
            }
        }

    // ==================== 验证码登录 ====================

    /**
     * 获取图形验证码，同时缓存本次登录所需的 execution token。
     * 每次调用都会重置会话，确保是一次干净的登录。
     */
    suspend fun fetchCaptchaImage(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            println("[Checkin] 开始获取验证码...")
            cookieStorage.clear()
            pendingSmsChallenge = null

            val loginPageResponse = api.getLoginPage()
            println("[Checkin] 登录页面响应状态: ${loginPageResponse.status}")
            if (loginPageResponse.status.value != 200) {
                return@withContext Result.failure(
                    CheckinException("无法访问登录页面 (${loginPageResponse.status.value})")
                )
            }

            val execution = UiasHtmlParser.execution(loginPageResponse.bodyAsText())
                ?: return@withContext Result.failure(CheckinException("未找到 execution token"))
            cachedExecution = execution
            println("[Checkin] execution token: ${execution.take(30)}...")

            val captchaImageBytes = api.getCaptchaImage()
            println("[Checkin] 验证码图片大小: ${captchaImageBytes.size} bytes")
            Result.success(captchaImageBytes)
        } catch (e: Exception) {
            println("[Checkin] 获取验证码异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 提交账号密码与图形验证码登录。
     * 若服务端要求短信二次验证，会以失败返回并记下上下文，
     * 调用方应据此走 [sendSmsCodeForPendingLogin] / [submitSmsCodeForPendingLogin]。
     */
    suspend fun loginWithCaptcha(
        username: String,
        password: String,
        captchaCode: String,
        accountId: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val execution = cachedExecution
                ?: return@withContext Result.failure(CheckinException("请先获取验证码"))

            println("[Checkin] 开始登录: username=$username, captcha=$captchaCode")

            // 密码需先反转再做 RSA 加密，这是 UIAS 前端的既有约定
            val encryptedPassword = CheckinRSAEncryptor.encrypt(
                password.reversed(),
                CheckinApiService.RSA_MODULUS,
                CheckinApiService.RSA_EXPONENT
            )

            pendingSmsChallenge = null

            val loginResponse = api.submitLogin(encryptedPassword, username, execution, captchaCode)
            println("[Checkin] 登录响应状态: ${loginResponse.status}")

            // 登录成功会以 302 重定向回业务系统
            if (loginResponse.status.value == 302) {
                val redirectUrl = loginResponse.headers[HttpHeaders.Location]
                    ?: return@withContext Result.failure(CheckinException("登录重定向失败"))
                println("[Checkin] 登录成功，重定向到: $redirectUrl")
                return@withContext finalizeLoginAfterRedirect(redirectUrl, accountId)
            }

            val responseBody = loginResponse.bodyAsText()
            println("[Checkin] 登录失败，响应长度: ${responseBody.length}")

            if (UiasHtmlParser.requiresSmsVerification(responseBody)) {
                pendingSmsChallenge = PendingSmsChallenge(
                    username = username,
                    execution = UiasHtmlParser.execution(responseBody) ?: execution,
                    phoneMasked = UiasHtmlParser.phoneMasked(responseBody),
                    accountId = accountId
                )
                return@withContext Result.failure(
                    CheckinException("$SMS_REQUIRED_MESSAGE，请输入短信验证码继续")
                )
            }

            pendingSmsChallenge = null
            val errorMsg = UiasHtmlParser.loginErrorMessage(responseBody, loginResponse.status.value)
            println("[Checkin] 错误原因: $errorMsg")
            Result.failure(CheckinException(errorMsg))
        } catch (e: Exception) {
            println("[Checkin] 登录异常: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== 短信二次验证 ====================

    /** 判断一次登录失败是否是因为需要短信二次验证 */
    fun isSmsVerificationRequired(error: Throwable?): Boolean {
        val message = error?.message ?: return false
        return message.contains(SMS_REQUIRED_MESSAGE) ||
            message.contains("doubleSubmit", ignoreCase = true) ||
            message.contains("smsCode", ignoreCase = true)
    }

    fun hasPendingSmsChallenge(): Boolean = pendingSmsChallenge != null

    fun getPendingSmsMaskedPhone(): String? = pendingSmsChallenge?.phoneMasked

    fun clearPendingSmsChallenge() {
        pendingSmsChallenge = null
    }

    /** 为待验证的登录发送短信验证码 */
    suspend fun sendSmsCodeForPendingLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val challenge = pendingSmsChallenge
                ?: return@withContext Result.failure(CheckinException("短信验证上下文已失效，请重新登录"))

            val response = api.sendSmsCode(challenge.username)
            if (response.status.value !in 200..299) {
                return@withContext Result.failure(
                    CheckinException("短信发送失败 (${response.status.value})")
                )
            }

            val responseBody = response.bodyAsText()
            val rejected = responseBody.contains("false", ignoreCase = true) ||
                responseBody.contains("失败") ||
                responseBody.contains("error", ignoreCase = true)
            if (rejected) {
                return@withContext Result.failure(CheckinException("短信发送失败，请稍后重试"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CheckinException("短信发送异常: ${e.message}"))
        }
    }

    /** 提交短信验证码完成登录 */
    suspend fun submitSmsCodeForPendingLogin(smsCode: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val challenge = pendingSmsChallenge
                    ?: return@withContext Result.failure(
                        CheckinException("短信验证上下文已失效，请重新登录")
                    )
                if (smsCode.isBlank()) {
                    return@withContext Result.failure(CheckinException("请输入短信验证码"))
                }

                val response = api.submitSmsVerification(
                    username = challenge.username,
                    execution = challenge.execution,
                    smsCode = smsCode,
                    phoneMasked = challenge.phoneMasked
                )

                if (response.status.value == 302) {
                    val redirectUrl = response.headers[HttpHeaders.Location]
                        ?: return@withContext Result.failure(
                            CheckinException("短信验证成功但重定向为空")
                        )
                    return@withContext finalizeLoginAfterRedirect(redirectUrl, challenge.accountId)
                }

                // 验证失败时页面会刷新 execution，续上上下文以便用户重试
                val responseBody = response.bodyAsText()
                UiasHtmlParser.execution(responseBody)?.takeIf { it.isNotBlank() }?.let { renewed ->
                    pendingSmsChallenge = challenge.copy(
                        execution = renewed,
                        phoneMasked = UiasHtmlParser.phoneMasked(responseBody)
                            ?: challenge.phoneMasked
                    )
                }

                val message = when {
                    responseBody.contains("验证码", ignoreCase = true) -> "短信验证码错误或已过期"
                    responseBody.contains("smsCode", ignoreCase = true) -> "短信验证码校验失败"
                    else -> "短信验证失败 (${response.status.value})"
                }
                Result.failure(CheckinException(message))
            } catch (e: Exception) {
                Result.failure(CheckinException("短信验证异常: ${e.message}"))
            }
        }

    // ==================== 内部实现 ====================

    /** 登录成功后跟随重定向，确认拿到 SESSION 并续期 rememberMe */
    private suspend fun finalizeLoginAfterRedirect(
        redirectUrl: String,
        accountId: Long?
    ): Result<Unit> {
        val finalResponse = api.followRedirect(redirectUrl)
        val sessionValue = qfhySessionCookie()
        if (sessionValue.isNullOrBlank()) {
            return Result.failure(CheckinException("登录成功但未获取到 SESSION，会话初始化失败"))
        }

        println(
            "[Checkin] 重定向完成，最终状态=${finalResponse.status.value}, " +
                "SESSION=${sessionValue.take(16)}..."
        )

        if (accountId != null) persistRememberMeCookies(accountId)

        cachedExecution = null
        pendingSmsChallenge = null
        return Result.success(Unit)
    }

    private fun qfhySessionCookie(): String? =
        cookieStorage.getCookiesForHost(QFHY_HOST)
            .find { it.name == "SESSION" }
            ?.value

    private fun hasQfhySessionCookie(): Boolean = !qfhySessionCookie().isNullOrBlank()

    /** 保存 UIAS 的 CASTGC，用于下次免密登录 */
    private fun persistRememberMeCookies(accountId: Long) {
        val uiasCookies = cookieStorage.getCookieString(UIAS_HOST)
        if (uiasCookies.isBlank() || !uiasCookies.contains("CASTGC=")) return
        accountStore.updateSession(
            accountId = accountId,
            sessionToken = uiasCookies,
            sessionExpireTime = CheckinClock.afterDays(REMEMBER_ME_VALID_DAYS)
        )
    }

    private companion object {
        const val UIAS_HOST = "uias.suse.edu.cn"
        const val QFHY_HOST = "qfhy.suse.edu.cn"
        const val SMS_REQUIRED_MESSAGE = "检测到短信二次验证"
        const val REMEMBER_ME_VALID_DAYS = 30
    }
}
