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
                // API 现已定义返回 ResponseBody，可以直接调用 string()
                val csrfHtml = api.getCSRFToken().string()
                val csrfToken = extractCSRFToken(csrfHtml)
                    ?: return@withContext Result.failure(Exception("无法获取 CSRF Token"))

                // 3. RSA 加密密码
                // API 现已定义返回 RSAKey 对象
                val rsaKey = api.getRSAKey()
                val encryptedPwd = RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)

                // 4. 执行登录
                val timestamp = System.currentTimeMillis().toString()
                val response = api.login(timestamp, username, encryptedPwd, csrfToken)

                // 5. 处理结果
                if (response.code() == 302) {
                    val location = response.headers()["Location"]
                    if (location != null) {
                        val targetUrl =
                            if (location.startsWith("/")) "https://jwgl.suse.edu.cn$location" else location
                        try {
                            api.visitUrl(targetUrl) // 访问跳转链接以完成 Cookie 设置
                            delay(500)
                            Result.success("登录成功")
                        } catch (e: Exception) {
                            Result.success("登录成功 (重定向异常: ${e.message})")
                        }
                    } else {
                        Result.success("登录成功 (无跳转)")
                    }
                } else {
                    val body = response.errorBody()?.string() ?: response.body()?.string() ?: ""
                    val msg =
                        if (body.contains("用户名或密码不正确")) "用户名或密码错误" else "登录失败，状态码: ${response.code()}"
                    Result.failure(Exception(msg))
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