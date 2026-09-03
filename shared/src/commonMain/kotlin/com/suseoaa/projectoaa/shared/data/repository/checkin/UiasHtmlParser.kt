package com.suseoaa.projectoaa.shared.data.repository.checkin

/**
 * UIAS 统一认证登录页的 HTML 解析。
 *
 * 登录流程是表单提交式的，需要从页面里抠出 execution token、短信验证的手机号掩码
 * 以及失败时的错误提示，这些正则集中放在这里，避免散落在登录逻辑中间。
 */
object UiasHtmlParser {

    private val EXECUTION = Regex("""name="execution"\s+value="([^"]+)"""")
    private val PHONE_MASKED = Regex("""name\s*=\s*"phone"[^>]*value\s*=\s*"([^"]+)"""")
    private val ERROR_BLOCK = Regex("""<div[^>]*class="[^"]*error[^"]*"[^>]*>([^<]+)</div>""")

    /** 提交登录表单必须携带的 execution token */
    fun execution(html: String): String? =
        EXECUTION.find(html)?.groupValues?.getOrNull(1)

    /** 短信二次验证页面上的手机号掩码 */
    fun phoneMasked(html: String): String? =
        PHONE_MASKED.find(html)?.groupValues?.getOrNull(1)

    /** 页面是否进入了短信二次验证环节 */
    fun requiresSmsVerification(html: String): Boolean =
        html.contains("smsCode", ignoreCase = true) ||
            html.contains("doubleSubmit", ignoreCase = true) ||
            html.contains("sendSms_double", ignoreCase = true) ||
            html.contains("短信", ignoreCase = true)

    /** 登录失败时的可读原因，优先取服务端返回的错误块 */
    fun loginErrorMessage(html: String, statusCode: Int): String {
        val serverError = ERROR_BLOCK.find(html)?.groupValues?.getOrNull(1)?.trim()
        return when {
            !serverError.isNullOrBlank() -> serverError
            html.contains("验证码错误") || html.contains("验证码不正确") -> "验证码错误"
            html.contains("密码") -> "用户名或密码错误"
            html.contains("用户") -> "用户名不存在"
            else -> "登录失败 ($statusCode)"
        }
    }
}
