package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import com.suseoaa.projectoaa.core.utils.RSAEncryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolAuthRepository @Inject constructor(
    private val api: SchoolApiService,
    private val cookieJar: SchoolCookieJar
) {
    suspend fun login(username: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 清除旧 Cookie
                cookieJar.clear()

                // 2. 获取 CSRF Token
                val csrfHtml = api.getCSRFToken().string()
                val csrfToken = extractCSRFToken(csrfHtml)
                    ?: return@withContext Result.failure(Exception("无法获取 CSRF Token"))

                // 3. RSA 加密密码
                val rsaKey = api.getRSAKey()
                val encryptedPwd = RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)

                // 4. 执行登录
                val timestamp = System.currentTimeMillis().toString()
                val response = api.login(timestamp, username, encryptedPwd, csrfToken)

                // 5. 获取最终页面内容（处理 302 跳转或 200 响应）
                val finalBody = if (response.code() == 302) {
                    val location = response.headers()["Location"] ?: ""
                    if (location.isNotEmpty()) {
                        val targetUrl = if (location.startsWith("/")) "https://jwgl.suse.edu.cn$location" else location
                        // 跟随跳转获取新页面内容
                        try {
                            api.visitUrl(targetUrl).string()
                        } catch (e: Exception) {
                            "" // 如果跳转失败，返回空字符串，后续逻辑会兜底
                        }
                    } else {
                        ""
                    }
                } else {
                    // 如果是 200，直接读取当前响应体
                    response.body()?.string() ?: response.errorBody()?.string() ?: ""
                }

                // 6. 统一判定逻辑：检查最终页面是否包含“登录页特征”或“错误提示”
                // 登录页特征：
                // 1. id="rsaKey" (用于加密密码，主页没有)
                // 2. id="tips" (错误提示框，您提供的失败HTML中有这个)
                // 3. name="mm" (密码输入框)

                val isLoginPage = finalBody.contains("id=\"rsaKey\"") ||
                        finalBody.contains("id=\"tips\"") ||
                        (finalBody.contains("name=\"yhm\"") && finalBody.contains("name=\"mm\""))

                if (isLoginPage) {
                    // 仍然停留在登录页，说明失败
                    val msg = when {
                        finalBody.contains("用户名或密码不正确") -> "用户名或密码错误"
                        finalBody.contains("验证码不正确") -> "验证码错误"
                        else -> "登录失败，请检查账号密码"
                    }
                    Result.failure(Exception(msg))
                } else {
                    // 已经跳出登录页（进入了主页或其他页面），视为成功
                    // 这里的逻辑是：只要不是登录页，就认为是进去了
                    delay(500) // 等待一下确保 Cookie 写入
                    Result.success("登录成功")
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun extractCSRFToken(html: String): String? {
        val patterns = listOf(
            Regex("""<input\s+type="hidden"\s+id="csrftoken"\s+name="csrftoken"\s+value="([^"]+)"\s*/>"""),
            Regex("""name="csrftoken"\s+value="([^"]+)""""),
            Regex("""id="csrftoken".*?value="([^"]+)"""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}