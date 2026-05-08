package com.suseoaa.projectoaa.shared.data.remote.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * 可清除的 Cookie 存储 (使用协程 Mutex 实现线程安全)
 */
class ClearableCookiesStorage : CookiesStorage {
    private val storage = mutableListOf<Cookie>()
    private val mutex = Mutex()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            // 移除同名旧 cookie
            storage.removeAll { it.name == cookie.name && it.domain == cookie.domain }

            if (cookie.value.equals("deleteMe", ignoreCase = true) || (cookie.maxAge ?: 1) <= 0) {
                println("[Cookie] 已剔除失效 Cookie: ${cookie.name}")
            } else {
                storage.add(cookie)
                println("[Cookie] 已添加 Cookie: ${cookie.name}=${cookie.value.take(20)}...")
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return mutex.withLock {
            val cookies = storage.filter { cookie ->
                // 简单匹配：检查域名和路径
                (cookie.domain.isNullOrEmpty() ||
                        requestUrl.host.endsWith(cookie.domain ?: "") ||
                        cookie.domain == requestUrl.host)
            }
            println("[Cookie] 读取域名 ${requestUrl.host} 的 Cookie: ${cookies.map { it.name }}")
            cookies
        }
    }

    override fun close() {
        storage.clear()
    }

    suspend fun clear() {
        mutex.withLock {
            println("[Cookie] 已清空全部 Cookie")
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

            // 添加默认请求头（不硬编码User-Agent）
            defaultRequest {
                if (!headers.contains("Accept-Language")) headers.append(
                    "Accept-Language",
                    "zh-CN,zh;q=0.9"
                )
                if (!headers.contains("Connection")) headers.append("Connection", "keep-alive")
            }

            // 允许手动控制认证重定向，因此禁用自动重定向
            followRedirects = false
        }
    }
}
