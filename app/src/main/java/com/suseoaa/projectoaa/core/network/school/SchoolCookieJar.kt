package com.suseoaa.projectoaa.core.network.school

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolCookieJar @Inject constructor() : Interceptor {

    // 使用内存 Map 存储 Cookie，Hilt 单例保证了它的持久性
    private val cookieMap: MutableMap<String, String> = mutableMapOf()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 1. 发送 Cookie
        if (cookieMap.isNotEmpty()) {
            val cookieString = cookieMap.map { "${it.key}=${it.value}" }.joinToString("; ")
            requestBuilder.addHeader("Cookie", cookieString)
        }

        // 2. 添加伪装头 (模拟浏览器)
        requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        requestBuilder.addHeader("Accept-Language", "zh-CN,zh;q=0.9")
        requestBuilder.addHeader("Connection", "keep-alive")

        val response = chain.proceed(requestBuilder.build())

        // 3. 保存 Cookie
        val cookieHeaders = response.headers("Set-Cookie")
        for (header in cookieHeaders) {
            // 解析 Set-Cookie: JSESSIONID=xxx; Path=/; HttpOnly
            val parts = header.split(";", limit = 2)[0].split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                // 忽略无效的空值
                if (key.isNotBlank() && value.isNotBlank() && value != "deleted") {
                    cookieMap[key] = value
                }
            }
        }
        return response
    }

    // 退出登录时调用
    fun clear() {
        cookieMap.clear()
    }
}