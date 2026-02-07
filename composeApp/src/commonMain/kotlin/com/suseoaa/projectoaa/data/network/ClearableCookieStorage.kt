package com.suseoaa.projectoaa.data.network

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.collections.*
import io.ktor.util.date.*

/**
 * 可清除的 Cookie 存储
 * 支持在每次新登录前清除所有 Cookie，避免多账号 Cookie 冲突
 */
class ClearableCookieStorage : CookiesStorage {

    private val storage = ConcurrentMap<String, MutableList<Cookie>>()

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val host = requestUrl.host
        return storage[host]?.filter { cookie ->
            // 检查 Cookie 是否过期
            val expires = cookie.expires
            expires == null || expires.timestamp > GMTDate().timestamp
        } ?: emptyList()
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        val host = requestUrl.host
        val list = storage.getOrPut(host) { mutableListOf() }

        // 移除同名的旧 Cookie
        list.removeAll { it.name == cookie.name }
        list.add(cookie)
    }

    override fun close() {
        storage.clear()
    }

    /**
     * 清除所有 Cookie
     * 在每次新登录前调用
     */
    fun clear() {
        storage.clear()
        println("[CookieStorage] 已清除所有 Cookie")
    }

    /**
     * 清除指定域名的 Cookie
     */
    fun clearForHost(host: String) {
        storage.remove(host)
        println("[CookieStorage] 已清除 $host 的 Cookie")
    }

    /**
     * 获取指定域名的所有 Cookie
     */
    fun getCookiesForHost(host: String): List<Cookie> {
        return storage[host]?.filter { cookie ->
            val expires = cookie.expires
            expires == null || expires.timestamp > GMTDate().timestamp
        } ?: emptyList()
    }

    /**
     * 获取指定域名的 Cookie 字符串
     */
    fun getCookieString(host: String): String {
        return getCookiesForHost(host).joinToString("; ") { "${it.name}=${it.value}" }
    }

    /**
     * 打印所有存储的 Cookie（用于调试）
     */
    fun debugPrintAllCookies() {
        println("[CookieStorage] 当前存储的所有 Cookie:")
        storage.forEach { (host, cookies) ->
            println("  Host: $host")
            cookies.forEach { cookie ->
                println("    ${cookie.name}=${cookie.value.take(50)}...")
            }
        }
    }
}
