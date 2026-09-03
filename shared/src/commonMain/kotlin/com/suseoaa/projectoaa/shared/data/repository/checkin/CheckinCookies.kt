package com.suseoaa.projectoaa.shared.data.repository.checkin

/**
 * Cookie 字符串工具。
 *
 * 签到链路里同时存在两种形态的 Cookie 文本：
 * - 请求用的 `Cookie` 头，形如 `a=1; b=2`
 * - 响应里的 `Set-Cookie` 头，形如 `SESSION=xxx; Path=/; HttpOnly`
 *
 * 两种取值逻辑之前散落在各处（含三处内联的 `Regex("SESSION=([^;]+)")`），统一到这里。
 */
object CheckinCookies {

    /** 从 `Cookie` 头形态的字符串中取某个 Cookie 的值 */
    fun valueOf(cookies: String, name: String): String? =
        cookies.split(";")
            .map { it.trim() }
            .find { it.startsWith("$name=") }
            ?.substringAfter("$name=")

    /** 从单条 `Set-Cookie` 中取某个 Cookie 的值 */
    fun fromSetCookie(setCookie: String, name: String): String? =
        Regex("$name=([^;]+)").find(setCookie)?.groupValues?.getOrNull(1)

    /** 从一批 `Set-Cookie` 中取第一个匹配的 Cookie 值 */
    fun fromSetCookies(setCookies: List<String>?, name: String): String? =
        setCookies?.firstNotNullOfOrNull { fromSetCookie(it, name) }

    /**
     * 把 `Set-Cookie` 里的 `名=值` 合并进已有的 Cookie 头字符串，已存在的不覆盖。
     * 用于手动跟随重定向时累积会话 Cookie。
     */
    fun merge(cookies: String, setCookie: String): String {
        val pair = setCookie.substringBefore(";").trim()
        val name = pair.substringBefore("=")
        return if (name.isBlank() || cookies.contains(name)) cookies else "$cookies; $pair"
    }
}
