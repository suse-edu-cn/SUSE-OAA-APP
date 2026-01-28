package com.suseoaa.projectoaa.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * 可清除的 Cookie 存储
 */
class ClearableCookiesStorage : CookiesStorage {
    private val storage = mutableListOf<Cookie>()
    
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        synchronized(storage) {
            // 移除同名旧 cookie
            storage.removeAll { it.name == cookie.name && it.domain == cookie.domain }
            storage.add(cookie)
            println("[Cookie] Added: ${cookie.name}=${cookie.value.take(20)}...")
        }
    }
    
    override suspend fun get(requestUrl: Url): List<Cookie> {
        return synchronized(storage) {
            val cookies = storage.filter { cookie ->
                // 简单匹配：检查域名和路径
                (cookie.domain.isNullOrEmpty() || 
                 requestUrl.host.endsWith(cookie.domain ?: "") || 
                 cookie.domain == requestUrl.host)
            }
            println("[Cookie] Get for ${requestUrl.host}: ${cookies.map { it.name }}")
            cookies
        }
    }
    
    override fun close() {
        synchronized(storage) {
            storage.clear()
        }
    }
    
    fun clear() {
        synchronized(storage) {
            println("[Cookie] Cleared all cookies")
            storage.clear()
        }
    }
}

object SchoolHttpClient {
    // 暴露 cookie storage 以便清除
    val cookieStorage = ClearableCookiesStorage()
    
    fun create(json: Json): HttpClient {
        return HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.DEFAULT
            }
            
            install(HttpCookies) {
                storage = cookieStorage
            }
            
            // 添加默认请求头（模拟浏览器）
            defaultRequest {
                headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                headers.append("Accept-Language", "zh-CN,zh;q=0.9")
                headers.append("Connection", "keep-alive")
                headers.append("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            }
            
            // Allow redirects but we might need to handle 302 manually for auth
            followRedirects = false 
        }
    }
}
