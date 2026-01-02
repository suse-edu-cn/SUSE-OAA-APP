package com.suseoaa.projectoaa.feature.course

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.suseoaa.projectoaa.core.network.course.CalendarAPI
import com.suseoaa.projectoaa.core.network.course.GetCSRFToken
import com.suseoaa.projectoaa.core.network.course.LoginAPI
import com.suseoaa.projectoaa.core.network.course.RedirectAPI
import com.suseoaa.projectoaa.core.network.course.RsaKeyAPI
import com.suseoaa.projectoaa.core.network.course.ScheduleAPI
import com.suseoaa.projectoaa.core.network.model.course.CourseResponseJson
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher


class ReceivedCookiesInterceptorFixed : Interceptor {
    companion object {
        // 使用MutableMap来存储Cookie，避免重复和覆盖
        private val cookieMap: MutableMap<String, String> = mutableMapOf()
        val cookies: List<String>
            get() = cookieMap.map { "${it.key}=${it.value}" }

        fun clearCookies() {
            cookieMap.clear()
        }

        fun addCookie(name: String, value: String) {
            cookieMap[name] = value
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 如果有存储的Cookie，自动添加到请求中
        val requestBuilder = originalRequest.newBuilder()
        if (cookieMap.isNotEmpty()) {
            val cookieString = cookies.joinToString("; ")
            requestBuilder.addHeader("Cookie", cookieString)
            println("发送请求Cookie: $cookieString")
        }

        val response = chain.proceed(requestBuilder.build())

        // 从响应头中获取Cookie并存储
        val cookieHeaders = response.headers("Set-Cookie")
        if (cookieHeaders.isNotEmpty()) {
            println("收到Set-Cookie头: $cookieHeaders")
            for (cookieHeader in cookieHeaders) {
                val cleanedCookie = cleanCookie(cookieHeader)
                val parts = cleanedCookie.split("=", limit = 2)
                if (parts.size == 2) {
                    val name = parts[0].trim()
                    val value = parts[1].trim()
                    cookieMap[name] = value
                    println("保存Cookie: $name=$value")
                }
            }
            println("当前所有Cookie: $cookies")
        }

        return response
    }

    private fun cleanCookie(cookieHeader: String): String {
        // 只取第一个分号之前的部分（键值对），去掉path、httponly等属性
        return cookieHeader.split(";")[0].trim()
    }
}

// 对密码进行RSA加密
object RSAEncryptorFixed {
    fun encrypt(plainText: String, modulusBase64: String, exponentBase64: String): String {
        try {
            // 解码Base64的modulus和exponent
            val modulusBytes = Base64.getDecoder().decode(modulusBase64)
            val exponentBytes = Base64.getDecoder().decode(exponentBase64)

            // 将字节转换为BigInteger
            val modulus = BigInteger(1, modulusBytes)
            val exponent = BigInteger(1, exponentBytes)

            // 创建公钥规格
            val keySpec = RSAPublicKeySpec(modulus, exponent)

            // 生成公钥
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            // 初始化加密器
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            // 加密数据
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 加密结果转为Base64字符串返回
            return Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("RSA加密失败: ${e.message}", e)
        }
    }
}

// 登录和课表查询
object SchoolSystem {
    // 使用单例Cookie拦截器，确保所有请求共享同一个Cookie存储
    private val cookieInterceptor = ReceivedCookiesInterceptorFixed()

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
            .addHeader(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Connection", "keep-alive")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .build()
        chain.proceed(newRequest)
    }

    private val loginInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // 如果是登录POST请求，添加特殊的请求头
        if (url.contains("/xtgl/login_slogin.html") && originalRequest.method == "POST") {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Referer", "https://jwgl.suse.edu.cn/xtgl/login_slogin.html")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    private val scheduleInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // 如果是课表查询的POST请求，添加特殊的AJAX请求头
        if (url.contains("/kbcx/xskbcx_cxXsKb.html") && originalRequest.method == "POST") {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader(
                    "Referer",
                    "https://jwgl.suse.edu.cn/kbcx/xskbcx_cxXsKb.html?gnmkdm=N2151"
                )
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(cookieInterceptor)
        .addInterceptor(headerInterceptor)
        .addInterceptor(loginInterceptor)
        .addInterceptor(scheduleInterceptor)
        .followRedirects(false) // 禁用自动重定向，手动处理
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jwgl.suse.edu.cn")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val loginAPI = retrofit.create(LoginAPI::class.java)
    private val scheduleAPI = retrofit.create(ScheduleAPI::class.java)

    // 登录功能
    suspend fun login(username: String, password: String): Pair<Boolean, String> {
        try {
            // 1. 清除之前的Cookie
            ReceivedCookiesInterceptorFixed.clearCookies()
            // 2. 获取登录页面和CSRF Token
            val tempCSRFAPI = retrofit.create(GetCSRFToken::class.java)
            val csrfResponse = tempCSRFAPI.getCSRFToken()

            val htmlContent = csrfResponse.string()
            val csrfToken = extractCSRFToken(htmlContent) ?: return Pair(false, "")

            // 3. 获取RSA公钥（使用同一个session）
            val rsaKeyAPI = retrofit.create(RsaKeyAPI::class.java)
            val rsaKey = rsaKeyAPI.getrsaKey()
            // 4. 加密密码
            val encryptedPassword =
                RSAEncryptorFixed.encrypt(password, rsaKey.modulus, rsaKey.exponent)
            // 5. 发送登录请求
            val timestamp = System.currentTimeMillis().toString()
            val response = loginAPI.login(timestamp, username, encryptedPassword, csrfToken)
            // 6. 检查登录结果
            if (response.code() == 302) {
                // 7. 处理重定向
                val location = response.headers()["Location"]
                if (location != null) {
                    val redirectUrl = if (location.startsWith("/")) {
                        "https://jwgl.suse.edu.cn$location"
                    } else {
                        location
                    }
                    try {
                        // 创建一个允许跟随重定向的临时客户端
                        val redirectClient = OkHttpClient.Builder()
                            .addInterceptor(cookieInterceptor)
                            .addInterceptor(headerInterceptor)
                            .followRedirects(true) // 允许自动跟随重定向
                            .followSslRedirects(true)
                            .build()

                        val redirectRetrofit = Retrofit.Builder()
                            .baseUrl("https://jwgl.suse.edu.cn")
                            .client(redirectClient)
                            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                            .build()

                        val redirectAPI = redirectRetrofit.create(RedirectAPI::class.java)
                        val redirectResponse = redirectAPI.visitUrl(redirectUrl)

                        val redirectContent = redirectResponse.string()
                        // 检查最终页面内容，确保不是错误页面
                        if (redirectContent.contains("登录", ignoreCase = true) &&
                            redirectContent.contains("用户名", ignoreCase = true)
                        ) {
                            return Pair(false, "登录验证失败")
                        }

                        // 关键：等待足够长的时间确保会话完全建立
                        delay(3000)
                        return Pair(true, "")

                    } catch (e: Exception) {
                        return Pair(true, "$e 登录可能成功")
                    }
                } else {
                    return Pair(true, "登录成功但无重定向")
                }
            } else {
                val responseBody = try {
                    response.errorBody()?.string() ?: response.body()?.string() ?: "无响应内容"
                } catch (e: Exception) {
                    "读取响应内容异常: ${e.message}"
                }
                return Pair(false, "登录失败，状态码: ${response.code()}")
            }

        } catch (e: Exception) {
            return Pair(false, "登录异常: ${e.message}\n堆栈: ${e.stackTraceToString()}")
        }
    }

    private fun extractCSRFToken(html: String): String? {
        val patterns = listOf(
            Regex("""<input\s+type="hidden"\s+id="csrftoken"\s+name="csrftoken"\s+value="([^"]+)"\s*/>"""),
            Regex("""name="csrftoken"\s+value="([^"]+)""""),
            Regex("""name="csrftoken"[^>]*value="([^"]+)"""")
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    // 查询课表
    suspend fun querySchedule(year: String, semester: String): Pair<String?, String> {
        try {
            // 1. 访问课表页面
            val pageResponse = scheduleAPI.getSchedulePage()
            if (pageResponse.code() == 302) {
                return Pair(null, "")
            }

            if (!pageResponse.isSuccessful) {
                return Pair(null, "")
            }

            // 2. 发送POST请求查询课表数据
            val response = scheduleAPI.querySchedule(
                year = year,
                semester = semester
            )
            if (response.code() == 302) {
                return Pair(null, "")
            }

            if (!response.isSuccessful) {
                return Pair(null, "")
            }

            val responseText = response.body()?.string() ?: ""
            return if (responseText.trim().isNotEmpty()) {
                Pair(responseText, "")
            } else {
                Pair(null, "")
            }

        } catch (e: Exception) {
            return Pair(null, "$e")
        }
    }

    // 解析课表数据
    suspend fun queryScheduleParsed(
        year: String,
        semester: String
    ): Pair<CourseResponseJson?, String> {
        val (rawData, debugInfo) = querySchedule(year, semester)

        return if (rawData != null) {
            try {
                val courseData = json.decodeFromString<CourseResponseJson>(rawData)
                Pair(courseData, "$debugInfo✓ JSON解析成功\n")
            } catch (e: Exception) {
                Pair(null, "$debugInfo✗ JSON解析失败: ${e.message}\n")
            }
        } else {
            Pair(null, debugInfo)
        }
    }

    private val calendarAPI = retrofit.create(CalendarAPI::class.java)

    //    获取当前学期的起始日期
    suspend fun fetchSemesterStart(): String? {
        try {
            val response = calendarAPI.getCalendar()
//            获取校历失败
            if (!response.isSuccessful) {
                return null
            }
            val html = response.body()?.string() ?: ""
//            使用正则表达式提取
            // 目标文本：2025-2026学年1学期(2025-09-08至2026-01-25)
            // 我们的策略：寻找 "(xxxx-xx-xx至" 这样的结构

            // Regex 解释：
            // \(          -> 匹配左括号
            // (\d{4}-\d{2}-\d{2}) -> 核心捕获组：匹配 2025-09-08 这种格式
            // 至          -> 匹配中文"至"
            val regex = Regex("""\((\d{4}-\d{2}-\d{2})至""")
//            搜索符合规则的文本
            val matchResult = regex.find(html)
            // groupValues[0] 是整个匹配到的字符串 "(2025-09-08至"
            // groupValues[1] 是我们在括号里捕获的日期 "2025-09-08"
            val dateStr = matchResult?.groupValues?.get(1)
            return dateStr
        } catch (e: Exception) {
            return null
        }
    }
}