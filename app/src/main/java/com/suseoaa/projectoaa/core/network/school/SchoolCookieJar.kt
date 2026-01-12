package com.suseoaa.projectoaa.core.network.school

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SchoolCookieJar @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Interceptor {

    // 2. 使用 SharedPreferences 持久化存储
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("school_cookies", Context.MODE_PRIVATE)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 3. 从 Prefs 读取 Cookie 发送给服务器
        val allCookies = prefs.all
        if (allCookies.isNotEmpty()) {
            val cookieString = allCookies.map { "${it.key}=${it.value}" }.joinToString("; ")
            requestBuilder.addHeader("Cookie", cookieString)
        }

        // 添加伪装头 (保持不变)
        requestBuilder.addHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        )
        requestBuilder.addHeader("Accept-Language", "zh-CN,zh;q=0.9")
        requestBuilder.addHeader("Connection", "keep-alive")

        val response = chain.proceed(requestBuilder.build())

        // 4. 保存服务器返回的新 Cookie 到 Prefs
        val cookieHeaders = response.headers("Set-Cookie")
        if (cookieHeaders.isNotEmpty()) {
            prefs.edit {
                for (header in cookieHeaders) {
                    // 解析 Set-Cookie: JSESSIONID=xxx; Path=/; HttpOnly
                    val parts = header.split(";", limit = 2)[0].split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        // 忽略无效的空值或删除标记
                        if (key.isNotBlank() && value.isNotBlank() && value != "deleted") {
                            putString(key, value)
                        }
                    }
                }
            } // 提交保存
        }
        return response
    }

    // 退出登录时调用 (清空本地存储)
    fun clear() {
        prefs.edit { clear() }
    }
}