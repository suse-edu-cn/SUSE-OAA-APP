package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.util.RSAEncryptor
import io.ktor.client.statement.*
import kotlinx.coroutines.delay

class SchoolAuthRepository(
    private val api: SchoolApiService
) {
    suspend fun login(username: String, password: String): Result<String> {
        return try {
            // 1. 获取 CSRF Token
            val csrfHtml = api.getCSRFToken().bodyAsText()
            val csrfToken = extractCSRFToken(csrfHtml)
                ?: return Result.failure(Exception("无法获取 CSRF Token"))

            // 2. RSA 加密密码
            val rsaKey = api.getRSAKey()
            val encryptedPwd = RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)

            // 3. 执行登录
            val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
            val response = api.login(timestamp, username, encryptedPwd, csrfToken)

            // 4. 处理响应
            val finalBody = if (response.status.value == 302) {
                val location = response.headers["Location"] ?: ""
                if (location.isNotEmpty()) {
                    val targetUrl = if (location.startsWith("/")) {
                        "https://jwgl.suse.edu.cn$location"
                    } else {
                        location
                    }
                    try {
                        api.visitUrl(targetUrl).bodyAsText()
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
            } else {
                response.bodyAsText()
            }

            // 5. 判断登录结果
            val isLoginPage = finalBody.contains("id=\"rsaKey\"") ||
                    finalBody.contains("id=\"tips\"") ||
                    (finalBody.contains("name=\"yhm\"") && finalBody.contains("name=\"mm\""))

            if (isLoginPage) {
                val msg = when {
                    finalBody.contains("用户名或密码不正确") -> "用户名或密码错误"
                    finalBody.contains("验证码不正确") -> "验证码错误"
                    else -> "登录失败，请检查账号密码"
                }
                Result.failure(Exception(msg))
            } else {
                delay(500)
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
