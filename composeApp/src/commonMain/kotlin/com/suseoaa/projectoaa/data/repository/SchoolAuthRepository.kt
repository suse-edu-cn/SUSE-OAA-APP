package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.network.SchoolHttpClient
import com.suseoaa.projectoaa.util.RSAEncryptor
import io.ktor.client.statement.*
import kotlinx.coroutines.delay

class SchoolAuthRepository(
    private val api: SchoolApiService
) {
    suspend fun login(username: String, password: String): Result<String> {
        return try {
            // 0. 清除旧 Cookie（重要！）
            SchoolHttpClient.cookieStorage.clear()
            
            // 1. 获取 CSRF Token
            val csrfResponse = api.getCSRFToken()
            val csrfHtml = csrfResponse.bodyAsText()
            println("[SchoolAuth] CSRF Response status: ${csrfResponse.status}")
            
            val csrfToken = extractCSRFToken(csrfHtml)
            if (csrfToken == null) {
                println("[SchoolAuth] Failed to extract CSRF token from HTML")
                return Result.failure(Exception("无法获取 CSRF Token，请检查网络连接"))
            }
            println("[SchoolAuth] CSRF Token: $csrfToken")

            // 2. RSA 加密密码
            val rsaKey = try {
                api.getRSAKey()
            } catch (e: Exception) {
                println("[SchoolAuth] Failed to get RSA key: ${e.message}")
                return Result.failure(Exception("获取加密密钥失败: ${e.message}"))
            }
            println("[SchoolAuth] RSA modulus: ${rsaKey.modulus.take(20)}...")
            
            val encryptedPwd = try {
                RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)
            } catch (e: Exception) {
                println("[SchoolAuth] RSA encryption failed: ${e.message}")
                return Result.failure(Exception("密码加密失败: ${e.message}"))
            }
            println("[SchoolAuth] Encrypted password length: ${encryptedPwd.length}")

            // 3. 执行登录
            val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
            val response = api.login(timestamp, username, encryptedPwd, csrfToken)
            println("[SchoolAuth] Login response status: ${response.status}")
            println("[SchoolAuth] Login response headers: ${response.headers.entries()}")

            // 4. 处理响应
            val finalBody = if (response.status.value == 302) {
                val location = response.headers["Location"] ?: ""
                println("[SchoolAuth] 302 redirect to: $location")
                if (location.isNotEmpty()) {
                    val targetUrl = if (location.startsWith("/")) {
                        "https://jwgl.suse.edu.cn$location"
                    } else {
                        location
                    }
                    try {
                        val redirectResponse = api.visitUrl(targetUrl)
                        println("[SchoolAuth] Redirect response status: ${redirectResponse.status}")
                        redirectResponse.bodyAsText()
                    } catch (e: Exception) {
                        println("[SchoolAuth] Redirect failed: ${e.message}")
                        ""
                    }
                } else {
                    ""
                }
            } else {
                response.bodyAsText()
            }
            
            println("[SchoolAuth] Final body length: ${finalBody.length}")
            println("[SchoolAuth] Final body contains rsaKey: ${finalBody.contains("id=\"rsaKey\"")}")
            println("[SchoolAuth] Final body contains tips: ${finalBody.contains("id=\"tips\"")}")

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
                println("[SchoolAuth] Login failed: $msg")
                Result.failure(Exception(msg))
            } else {
                println("[SchoolAuth] Login successful!")
                delay(300)
                Result.success("登录成功")
            }
        } catch (e: Exception) {
            println("[SchoolAuth] Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("网络请求失败: ${e.message}"))
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
                println("[SchoolAuth] CSRF pattern matched: ${pattern.pattern}")
                return match.groupValues[1]
            }
        }
        return null
    }
}
