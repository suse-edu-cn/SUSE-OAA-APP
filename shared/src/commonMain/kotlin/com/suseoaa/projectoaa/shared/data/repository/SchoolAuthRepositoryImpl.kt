package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.data.remote.network.SchoolHttpClient
import com.suseoaa.projectoaa.shared.util.RSAEncryptor
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository

class SchoolAuthRepositoryImpl(
    private val api: SchoolApiService
) : SchoolAuthRepository {
    companion object {
        // 登录互斥锁，防止并发登录导致 cookie 冲突
        private val loginMutex = Mutex()

        // 登录状态缓存：记录上次成功登录的用户和时间
        private var lastLoginUsername: String? = null
        private var lastLoginTime: Long = 0L

        // Session 有效期：5分钟（教务系统 session 通常更长，但我们保守一点）
        private const val SESSION_VALIDITY_MS = 5 * 60 * 1000L
    }

    /**
     * 检查当前 session 是否仍然有效
     */
    private fun isSessionValid(username: String): Boolean {
        val now = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
        val isValid = lastLoginUsername == username &&
                (now - lastLoginTime) < SESSION_VALIDITY_MS
        AppLog.d(
            "[SchoolAuth] Session检查: username=$username, lastUser=$lastLoginUsername, " +
                    "elapsed=${now - lastLoginTime}ms, valid=$isValid"
        )
        return isValid
    }

    /**
     * 标记登录成功
     */
    private fun markLoginSuccess(username: String) {
        lastLoginUsername = username
        lastLoginTime = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
        AppLog.d("[SchoolAuth] 已标记登录成功: username=$username, time=$lastLoginTime")
    }

    /**
     * 使当前 session 失效（当检测到需要重新登录时调用）
     */
    override fun invalidateSession() {
        AppLog.d("[SchoolAuth] Session已失效")
        lastLoginUsername = null
        lastLoginTime = 0L
    }

    override suspend fun login(username: String, password: String): Result<String> = loginMutex.withLock {
        AppLog.d("[SchoolAuth] ========== 开始登录流程 (已获取锁) ==========")
        AppLog.d("[SchoolAuth] username=$username, password length=${password.length}")

        // 检查是否可以复用现有 session
        if (isSessionValid(username)) {
            AppLog.d("[SchoolAuth] ✓ 复用现有 Session，跳过登录")
            return@withLock Result.success("登录成功（复用Session）")
        }

        try {
            // 0. 清除旧 Cookie（重要！）
            AppLog.d("[SchoolAuth] 清除旧 Cookie")
            SchoolHttpClient.cookieStorage.clear()

            // 1. 获取 CSRF Token
            AppLog.d("[SchoolAuth] 获取 CSRF Token...")
            val csrfResponse = api.getCSRFToken()
            val csrfHtml = csrfResponse.bodyAsText()
            AppLog.d("[SchoolAuth] CSRF Response status: ${csrfResponse.status}")

            val csrfToken = extractCSRFToken(csrfHtml)
            if (csrfToken == null) {
                AppLog.e("[SchoolAuth] Failed to extract CSRF token from HTML")
                AppLog.d("[SchoolAuth] CSRF HTML Snippet: ${csrfHtml.take(500)}")
                return appFailure(AppError.Business(userMessage = "无法获取 CSRF Token，请检查网络连接"))
            }
            AppLog.d("[SchoolAuth] CSRF Token: $csrfToken")

            // 2. RSA 加密密码
            val rsaKey = try {
                api.getRSAKey()
            } catch (e: Exception) {
                AppLog.e("[SchoolAuth] Failed to get RSA key: ${e.message}")
                return appFailure(AppError.Business(userMessage = "获取加密密钥失败: ${e.message}"))
            }
            AppLog.d("[SchoolAuth] RSA modulus: ${rsaKey.modulus.take(20)}...")

            val encryptedPwd = try {
                RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)
            } catch (e: Exception) {
                AppLog.e("[SchoolAuth] RSA encryption failed: ${e.message}")
                return appFailure(AppError.Business(userMessage = "密码加密失败: ${e.message}"))
            }
            AppLog.d("[SchoolAuth] Encrypted password length: ${encryptedPwd.length}")

            // 3. 执行登录
            val timestamp = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds().toString()
            val response = api.login(timestamp, username, encryptedPwd, csrfToken)
            AppLog.d("[SchoolAuth] Login response status: ${response.status}")
            AppLog.d("[SchoolAuth] Login response headers: ${response.headers.entries()}")

            // 4. 处理响应
            val finalBody = if (response.status.value == 302) {
                val location = response.headers["Location"] ?: ""
                AppLog.d("[SchoolAuth] 302 redirect to: $location")
                if (location.isNotEmpty()) {
                    val targetUrl = if (location.startsWith("/")) {
                        "${ApiConfig.SCHOOL_BASE}$location"
                    } else {
                        location
                    }
                    try {
                        val redirectResponse = api.visitUrl(targetUrl)
                        AppLog.d("[SchoolAuth] Redirect response status: ${redirectResponse.status}")
                        redirectResponse.bodyAsText()
                    } catch (e: Exception) {
                        AppLog.e("[SchoolAuth] Redirect failed: ${e.message}")
                        ""
                    }
                } else {
                    ""
                }
            } else {
                response.bodyAsText()
            }

            AppLog.d("[SchoolAuth] Final body length: ${finalBody.length}")
            AppLog.d("[SchoolAuth] Final body contains rsaKey: ${finalBody.contains("id=\"rsaKey\"")}")
            AppLog.d("[SchoolAuth] Final body contains tips: ${finalBody.contains("id=\"tips\"")}")

            // 5. 判断登录结果
            val isLoginPage = finalBody.contains("id=\"rsaKey\"") ||
                    finalBody.contains("id=\"tips\"") ||
                    (finalBody.contains("name=\"yhm\"") && finalBody.contains("name=\"mm\""))

            if (isLoginPage) {
                val msg = when {
                    finalBody.contains("用户名或密码不正确") -> "用户名或密码错误"
                    finalBody.contains("验证码不正确") -> "验证码错误"
                    finalBody.contains("该账号已被锁定") -> "账号已被锁定，请稍后再试"
                    else -> {
                        // 尝试提取错误提示
                        val tipMatch = Regex("""<p id="tips"[^>]*>([^<]+)</p>""").find(finalBody)
                        tipMatch?.groupValues?.get(1)?.trim() ?: "登录失败，请检查账号密码"
                    }
                }
                AppLog.e("[SchoolAuth] Login failed: $msg")
                appFailure(AppError.Business(userMessage = msg))
            } else {
                AppLog.d("[SchoolAuth] Login successful!")
                markLoginSuccess(username)
                delay(300)
                Result.success("登录成功")
            }
        } catch (e: Exception) {
            AppLog.e("[SchoolAuth] Exception: ${e.message}")
            e.printStackTrace()
            appFailure(AppError.Business(userMessage = "网络请求失败: ${e.message}"))
        }
    }

    private fun extractCSRFToken(html: String): String? {
        val patterns = listOf(
            Regex("""<input\s+type="hidden"\s+id="csrftoken"\s+name="csrftoken"\s+value="([^"]+)"\s*/>"""),
            Regex("""name="csrftoken"\s+value="([^"]+)""""),
            Regex("""id="csrftoken".*?value="([^"]+)""""),
            Regex("""value="([^"]+)".*?name="csrftoken"""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                AppLog.d("[SchoolAuth] CSRF pattern matched: ${pattern.pattern}")
                return match.groupValues[1]
            }
        }
        return null
    }
}
