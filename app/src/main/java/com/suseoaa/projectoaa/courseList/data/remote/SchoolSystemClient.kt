package com.suseoaa.projectoaa.courseList.data.remote

import com.suseoaa.projectoaa.courseList.data.api.RsaKeyAPI
import com.suseoaa.projectoaa.courseList.data.api.GetCSRFToken
import com.suseoaa.projectoaa.courseList.data.api.RedirectAPI
import com.suseoaa.projectoaa.courseList.data.api.LoginAPI
import com.suseoaa.projectoaa.courseList.data.api.ScheduleAPI
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.suseoaa.projectoaa.courseList.data.remote.dto.CourseResponseJson
import kotlinx.coroutines.delay
import okhttp3.Response
import retrofit2.HttpException
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

//TODO 对密码进行RSA加密
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

//TODO 登录和课表查询
object SchoolSystem {
    // 使用单例Cookie拦截器，确保所有请求共享同一个Cookie存储
    private val cookieInterceptor = ReceivedCookiesInterceptorFixed()

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
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
                .addHeader("Referer", "https://jwgl.suse.edu.cn/kbcx/xskbcx_cxXsKb.html?gnmkdm=N2151")
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

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jwgl.suse.edu.cn")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val loginAPI = retrofit.create(LoginAPI::class.java)
    private val scheduleAPI = retrofit.create(ScheduleAPI::class.java)

    // 修复后的登录功能
    suspend fun login(username: String, password: String): Pair<Boolean, String> {
        try {
            var debugInfo = ""

            // 1. 清除之前的Cookie
            ReceivedCookiesInterceptorFixed.clearCookies()
            debugInfo += "步骤1: 清除旧Cookie\n"

            // 2. 获取登录页面和CSRF Token（完全模拟Python的session.get()）
            debugInfo += "步骤2: 获取登录页面和CSRF Token\n"
            val tempCSRFAPI = retrofit.create(GetCSRFToken::class.java)
            val csrfResponse = tempCSRFAPI.getCSRFToken()

            val htmlContent = csrfResponse.string()
            val csrfToken = extractCSRFToken(htmlContent)

            debugInfo += "登录页面访问成功\n"
            debugInfo += "CSRF Token: ${csrfToken?.take(20)}...\n"
            debugInfo += "获取登录页面后的Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

            if (csrfToken == null) {
                return Pair(false, "$debugInfo✗ 未找到CSRF Token")
            }

            // 3. 获取RSA公钥（使用同一个session）
            debugInfo += "步骤3: 获取RSA公钥\n"
            val rsaKeyAPI = retrofit.create(RsaKeyAPI::class.java)
            val rsaKey = rsaKeyAPI.getrsaKey()
            debugInfo += "RSA公钥获取成功\n"
            debugInfo += "获取公钥后的Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

            // 4. 加密密码
            debugInfo += "步骤4: 加密密码\n"
            val encryptedPassword = RSAEncryptorFixed.encrypt(password, rsaKey.modulus, rsaKey.exponent)
            debugInfo += "密码加密成功\n"

            // 5. 发送登录请求（完全模拟Python的session.post()）
            val timestamp = System.currentTimeMillis().toString()
            debugInfo += "步骤5: 发送登录请求\n"
            debugInfo += "登录前的Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

            val response = loginAPI.login(timestamp, username, encryptedPassword, csrfToken)

            debugInfo += "登录响应状态码: ${response.code()}\n"
            debugInfo += "登录响应头: ${response.headers()}\n"

            // 6. 检查登录结果
            if (response.code() == 302) {
                debugInfo += "✓ 收到302重定向，登录成功\n"

                // 7. 处理重定向（完全模拟Python的session.get(redirect_url)）
                val location = response.headers()["Location"]
                debugInfo += "重定向地址: $location\n"
                debugInfo += "登录后收到的Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

                if (location != null) {
                    val redirectUrl = if (location.startsWith("/")) {
                        "https://jwgl.suse.edu.cn$location"
                    } else {
                        location
                    }

                    debugInfo += "访问重定向URL: $redirectUrl\n"

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
                            .addConverterFactory(MoshiConverterFactory.create(moshi))
                            .build()

                        val redirectAPI = redirectRetrofit.create(RedirectAPI::class.java)
                        val redirectResponse = redirectAPI.visitUrl(redirectUrl)

                        val redirectContent = redirectResponse.string()
                        debugInfo += "✓ 重定向页面访问成功（已跟随重定向链）\n"
                        debugInfo += "最终页面长度: ${redirectContent.length}\n"
                        debugInfo += "访问重定向页面后的Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

                        // 检查最终页面内容，确保不是错误页面
                        if (redirectContent.contains("登录", ignoreCase = true) &&
                            redirectContent.contains("用户名", ignoreCase = true)) {
                            debugInfo += "⚠ 最终页面似乎还是登录页面，可能登录失败\n"
                            debugInfo += "页面内容片段: ${redirectContent.take(200)}\n"
                            return Pair(false, "$debugInfo✗ 登录验证失败")
                        }

                        // 关键：等待足够长的时间确保会话完全建立
                        delay(3000)
                        debugInfo += "等待3秒确保会话完全生效\n"
                        debugInfo += "最终Cookie状态: ${ReceivedCookiesInterceptorFixed.cookies}\n"

                        return Pair(true, "$debugInfo✓ 登录流程完成")

                    } catch (e: Exception) {
                        debugInfo += "重定向访问异常: ${e.message}\n"
                        debugInfo += "异常类型: ${e::class.java.simpleName}\n"

                        // 如果是HTTP异常，尝试获取更多信息
                        if (e is HttpException) {
                            debugInfo += "HTTP错误码: ${e.code()}\n"
                            debugInfo += "HTTP错误消息: ${e.message()}\n"
                        }

                        debugInfo += "异常堆栈: ${e.stackTraceToString()}\n"

                        // 重定向失败可能是正常的，登录可能仍然成功
                        // 继续尝试而不是直接返回失败
                        debugInfo += "⚠ 重定向处理失败，但登录可能已成功，继续尝试\n"
                        delay(2000)
                        return Pair(true, "$debugInfo⚠ 登录可能成功（重定向异常但继续）")
                    }
                } else {
                    debugInfo += "⚠ 没有重定向地址\n"
                    return Pair(true, "$debugInfo⚠ 登录成功但无重定向")
                }
            } else {
                val responseBody = try {
                    response.errorBody()?.string() ?: response.body()?.string() ?: "无响应内容"
                } catch (e: Exception) {
                    "读取响应内容异常: ${e.message}"
                }
                debugInfo += "登录失败，响应内容前300字符: ${responseBody.take(300)}\n"
                return Pair(false, "$debugInfo✗ 登录失败，状态码: ${response.code()}")
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
    suspend fun querySchedule(): Pair<String?, String> {
        var debugInfo = ""
        try {
            debugInfo += "开始课表查询流程\n"
            debugInfo += "当前Cookies: ${ReceivedCookiesInterceptorFixed.cookies}\n"

            // 1. 访问课表页面
            debugInfo += "步骤1: 访问课表查询页面\n"
            val pageResponse = scheduleAPI.getSchedulePage()

            debugInfo += "课表页面响应状态码: ${pageResponse.code()}\n"

            if (pageResponse.code() == 302) {
                val location = pageResponse.headers()["Location"]
                debugInfo += "✗ 访问课表页面被重定向到: $location\n"
                return Pair(null, debugInfo)
            }

            if (!pageResponse.isSuccessful) {
                debugInfo += "✗ 课表页面访问失败，状态码: ${pageResponse.code()}\n"
                return Pair(null, debugInfo)
            }

//            val pageContent = pageResponse.body()?.string() ?: ""
//            debugInfo += "✓ 课表页面访问成功，长度: ${pageContent.length}\n"

            // 2. 发送POST请求查询课表数据
            debugInfo += "步骤2: 发送POST请求查询课表数据\n"
            val response = scheduleAPI.querySchedule()

            debugInfo += "课表查询响应状态码: ${response.code()}\n"

            if (response.code() == 302) {
                val location = response.headers()["Location"]
                debugInfo += "✗ 课表查询被重定向到: $location\n"
                return Pair(null, debugInfo)
            }

            if (!response.isSuccessful) {
                debugInfo += "✗ 课表查询失败，状态码: ${response.code()}\n"
                return Pair(null, debugInfo)
            }

            val responseText = response.body()?.string() ?: ""
            debugInfo += "课表查询响应长度: ${responseText.length}\n"
            debugInfo += "课表查询响应内容: ${responseText}\n"

            return if (responseText.trim().isNotEmpty()) {
                debugInfo += "✓ 成功获取课表数据\n"
                Pair(responseText, debugInfo)
            } else {
                debugInfo += "✗ 课表数据为空\n"
                Pair(null, debugInfo)
            }

        } catch (e: Exception) {
            debugInfo += "课表查询异常: ${e.message}\n"
            return Pair(null, debugInfo)
        }
    }

    // 新增：解析课表数据的方法
    suspend fun queryScheduleParsed(): Pair<CourseResponseJson?, String> {
        val (rawData, debugInfo) = querySchedule()

        return if (rawData != null) {
            try {
                val adapter = moshi.adapter(CourseResponseJson::class.java)
                val courseData = adapter.fromJson(rawData)
                Pair(courseData, "$debugInfo✓ JSON解析成功\n")
            } catch (e: Exception) {
                Pair(null, "$debugInfo✗ JSON解析失败: ${e.message}\n")
            }
        } else {
            Pair(null, debugInfo)
        }
    }
}