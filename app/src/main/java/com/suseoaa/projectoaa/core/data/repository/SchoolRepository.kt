package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.network.model.course.CourseResponseJson
import com.suseoaa.projectoaa.core.network.school.SchoolApiService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import com.suseoaa.projectoaa.core.utils.RSAEncryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val api: SchoolApiService,
    private val cookieJar: SchoolCookieJar,
    private val json: Json // 注入全局 Json 实例
) {

    /**
     * 登录教务系统
     * @return Result<String> 成功返回成功消息，失败返回异常
     */
    suspend fun login(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            cookieJar.clear() // 1. 清空旧 Cookie

            // 2. 获取 CSRF Token
            val csrfHtml = api.getCSRFToken().string()
            val csrfToken = extractCSRFToken(csrfHtml)
                ?: return@withContext Result.failure(Exception("无法获取 CSRF Token"))

            // 3. 获取 RSA 公钥并加密
            val rsaKey = api.getRSAKey()
            val encryptedPwd = RSAEncryptor.encrypt(password, rsaKey.modulus, rsaKey.exponent)

            // 4. 发送登录请求
            val timestamp = System.currentTimeMillis().toString()
            val response = api.login(timestamp, username, encryptedPwd, csrfToken)

            // 5. 处理 302 重定向 (标志登录成功)
            if (response.code() == 302) {
                val location = response.headers()["Location"]
                if (location != null) {
                    val targetUrl = if (location.startsWith("/")) "https://jwgl.suse.edu.cn$location" else location
                    try {
                        // 访问重定向地址以激活 Session
                        api.visitUrl(targetUrl)
                        delay(500) // 稍作等待确保 Session 生效
                        Result.success("登录成功")
                    } catch (e: Exception) {
                        // 即使重定向访问出错，Session 可能已经建立，姑且认为成功
                        Result.success("登录成功 (重定向异常: ${e.message})")
                    }
                } else {
                    Result.success("登录成功 (无跳转)")
                }
            } else {
                // 如果返回 200，通常是登录失败停留在原页面，尝试解析错误信息
                val body = response.errorBody()?.string() ?: response.body()?.string() ?: ""
                val msg = if (body.contains("用户名或密码不正确")) "用户名或密码错误" else "登录失败，状态码: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取课表数据 (已解析为对象)
     */
    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> = withContext(Dispatchers.IO) {
        try {
            val response = api.querySchedule(year = year, semester = semester)
            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: ""
                try {
                    val data = json.decodeFromString<CourseResponseJson>(jsonString)
                    Result.success(data)
                } catch (e: Exception) {
                    Result.failure(Exception("JSON 解析失败: ${e.message}"))
                }
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取开学日期 (YYYY-MM-DD)
     */
    suspend fun fetchSemesterStart(): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getCalendar()
            if (!response.isSuccessful) return@withContext null

            val html = response.body()?.string() ?: ""
            // 优化后的正则：匹配 "2025-09-08 至" 格式
            val regex = Regex("""(\d{4}-\d{2}-\d{2})\s*至""")
            regex.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractCSRFToken(html: String): String? {
        // 尝试多种正则匹配
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