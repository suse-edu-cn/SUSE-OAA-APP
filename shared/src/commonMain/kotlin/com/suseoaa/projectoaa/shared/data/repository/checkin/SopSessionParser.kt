package com.suseoaa.projectoaa.shared.data.repository.checkin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** 从 `_sop_session_` JWT 中解析出来的用户身份 */
data class SopSessionUser(
    val studentId: String,
    val name: String
)

/**
 * `_sop_session_` JWT 解析器。
 *
 * JWT 的 payload 结构：
 * ```json
 * {
 *   "uid": "23341010304",
 *   "ticket": "xxx",
 *   "extra": "{\"userName\":\"张三\",\"openId\":\"oXL_x6...\",\"identityType\":1}"
 * }
 * ```
 * 注意 `extra` 是一段嵌套的 JSON **字符串**，openId 与姓名都在里面。
 *
 * 这段 Base64Url 解码逻辑原先在 CheckinRepository 和 QrCodeCheckinRepository 里
 * 各自复制了七份，现在统一由本类提供。
 */
class SopSessionParser(private val json: Json) {

    /** 从完整 Cookie 字符串中取出 `_sop_session_` 的值 */
    fun sopSessionValueOf(cookies: String): String? =
        CheckinCookies.valueOf(cookies, SOP_SESSION)

    /** 取 ticket（SSO 换取 SESSION 时使用） */
    fun ticketOf(jwt: String): String? =
        payloadOf(jwt)?.get("ticket")?.jsonPrimitive?.content

    /** 取微信 openId，它位于 payload.extra 内 */
    fun openIdOf(jwt: String): String? =
        extraOf(jwt)?.get("openId")?.jsonPrimitive?.content

    /** 直接从完整 Cookie 字符串中取微信 openId */
    fun openIdFromCookies(cookies: String): String? =
        sopSessionValueOf(cookies)?.let { openIdOf(it) }

    /** 取学号与姓名；姓名缺失时返回空串 */
    fun userInfoOf(jwt: String): SopSessionUser? {
        val payload = payloadOf(jwt) ?: return null
        val uid = payload["uid"]?.jsonPrimitive?.content
        if (uid.isNullOrBlank()) {
            println("[SopSession] payload 中缺少 uid")
            return null
        }
        val name = extraOf(jwt)?.get("userName")?.jsonPrimitive?.content.orEmpty()
        return SopSessionUser(studentId = uid, name = name)
    }

    /** 直接从完整 Cookie 字符串中取学号与姓名 */
    fun userInfoFromCookies(cookies: String): SopSessionUser? =
        sopSessionValueOf(cookies)?.let { userInfoOf(it) }

    /** 解码 JWT 的 payload 段 */
    @OptIn(ExperimentalEncodingApi::class)
    private fun payloadOf(jwt: String): JsonObject? {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) {
                println("[SopSession] 不是有效的 JWT，段数=${parts.size}")
                return null
            }
            val payload = parts[1]
            // Base64Url 编码在 JWT 中省略了尾部补位，这里补回来再解码
            val padded = when (payload.length % 4) {
                2 -> "$payload=="
                3 -> "$payload="
                else -> payload
            }
            val decoded = Base64.UrlSafe.decode(padded).decodeToString()
            json.parseToJsonElement(decoded).jsonObject
        } catch (e: Exception) {
            println("[SopSession] 解析 JWT payload 失败: ${e.message}")
            null
        }
    }

    /** 解析 payload.extra 这层嵌套 JSON 字符串 */
    private fun extraOf(jwt: String): JsonObject? {
        val extraString = payloadOf(jwt)?.get("extra")?.jsonPrimitive?.content
        if (extraString.isNullOrBlank()) return null
        return try {
            json.parseToJsonElement(extraString).jsonObject
        } catch (e: Exception) {
            println("[SopSession] 解析 extra 失败: ${e.message}")
            null
        }
    }

    private companion object {
        const val SOP_SESSION = "_sop_session_"
    }
}
