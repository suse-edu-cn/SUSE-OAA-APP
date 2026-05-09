package com.suseoaa.projectoaa.shared.data.remote.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * 652打卡 API 服务
 * 基于统一认证系统(UIAS)进行登录，然后访问勤风化雨系统进行打卡
 *
 * 打卡流程：
 * 1. 登录 UIAS 统一认证系统
 * 2. 获取待签到任务列表 (GET /site/qddk/qdrw/api/myList.rst?status=1)
 * 3. 获取任务详情 (GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId})
 * 4. 提交位置签到 (POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst)
 */
class CheckinApiService(val httpClient: HttpClient) {

    companion object {
        private const val UIAS_BASE = "https://uias.suse.edu.cn"
        private const val QFHY_BASE = "https://qfhy.suse.edu.cn"
        private const val QDDK_ADMIN_ENTRY = "$QFHY_BASE/xg/app/qddk/admin/qddkdk"

        // 登录服务地址
        private val LOGIN_SERVICE =
            "$QFHY_BASE/site/appware/system/sso/login?target=$QDDK_ADMIN_ENTRY"
        private val LOGIN_PAGE = "$UIAS_BASE/sso/login?service=${LOGIN_SERVICE.encodeURLParameter()}"

        // 验证码地址
        private const val CAPTCHA_URL = "$UIAS_BASE/sso/captcha.jpg"
        private const val SMS_SEND_URL = "$UIAS_BASE/sso/smsLogin/sendSms_double"

        // API 地址
        private const val USER_GROUPS_URL =
            "$QFHY_BASE/site/app/base/common/api/user/groups.rst?appCode=qddk"
        private const val TASK_LIST_BASE_URL = "$QFHY_BASE/site/qddk/qdrw/api/myList.rst"
        private const val CHECKIN_BASE_URL = "$QFHY_BASE/site/app/base/common/api/group"

        // RSA 密钥（固定值）
        const val RSA_MODULUS =
            "008aed7e057fe8f14c73550b0e6467b023616ddc8fa91846d2613cdb7f7621e3cada4cd5d812d627af6b87727ade4e26d26208b7326815941492b2204c3167ab2d53df1e3a2c9153bdb7c8c2e968df97a5e7e01cc410f92c4c2c2fba529b3ee988ebc1fca99ff5119e036d732c368acf8beba01aa2fdafa45b21e4de4928d0d403"
        const val RSA_EXPONENT = "010001"
    }

    /**
     * 获取登录页面以提取 execution token
     */
    suspend fun getLoginPage(loginCookies: String? = null): HttpResponse {
        return httpClient.get(LOGIN_PAGE) {
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            )
            header("Accept-Language", "zh-CN,zh;q=0.9")
            if (!loginCookies.isNullOrBlank()) {
                header("Cookie", loginCookies)
            }
        }
    }

    /**
     * 获取验证码图片
     * @return 验证码图片的字节数组
     */
    suspend fun getCaptchaImage(): ByteArray {
        val response = httpClient.get(CAPTCHA_URL) {
            header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Referer", LOGIN_PAGE)
        }
        return response.readRawBytes()
    }

    /**
     * 提交登录表单（包含验证码）
     */
    suspend fun submitLogin(
        encryptedPassword: String,
        username: String,
        execution: String,
        captchaCode: String
    ): HttpResponse {
        return httpClient.submitForm(
            url = LOGIN_PAGE,
            formParameters = Parameters.build {
                append("username", username)
                append("password", encryptedPassword)
                append("authcode", captchaCode)
                append("execution", execution)
                append("encrypted", "true")
                append("_eventId", "submit")
                append("loginType", "1")
                append("rememberMe", "true")
            }
        ) {
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            )
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Origin", UIAS_BASE)
            header("Referer", LOGIN_PAGE)
        }
    }

    /**
     * 发送短信验证码（新HAR二次验证流程）
     */
    suspend fun sendSmsCode(username: String): HttpResponse {
        return httpClient.submitForm(
            url = SMS_SEND_URL,
            formParameters = Parameters.build {
                append("request_username", username)
                append("type", "1")
            }
        ) {
            header("Accept", "*/*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Origin", UIAS_BASE)
            header("Referer", LOGIN_PAGE)
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 提交短信二次验证
     */
    suspend fun submitSmsVerification(
        username: String,
        execution: String,
        smsCode: String,
        phoneMasked: String? = null
    ): HttpResponse {
        return httpClient.submitForm(
            url = LOGIN_PAGE,
            formParameters = Parameters.build {
                if (!phoneMasked.isNullOrBlank()) {
                    append("phone", phoneMasked)
                }
                append("username", username)
                append("smsCode", smsCode)
                append("execution", execution)
                append("encrypted", "true")
                append("_eventId", "doubleSubmit")
                append("loginType", "2")
                append("rememberMe", "true")
            }
        ) {
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            )
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Origin", UIAS_BASE)
            header("Referer", LOGIN_PAGE)
        }
    }

    /**
     * 跟随重定向获取 Session
     */
    suspend fun followRedirect(redirectUrl: String, maxRedirects: Int = 5): HttpResponse {
        var currentUrl = redirectUrl
        var referer = "$UIAS_BASE/"
        var response = httpClient.get(currentUrl) {
            header("Referer", referer)
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            )
        }

        var redirectCount = 0
        while (response.status.value in 300..399 && redirectCount < maxRedirects) {
            val location = response.headers[HttpHeaders.Location] ?: break
            val resolvedUrl = resolveRedirectUrl(currentUrl, location)
            referer = currentUrl
            currentUrl = resolvedUrl

            response = httpClient.get(currentUrl) {
                header("Referer", referer)
                header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
                )
            }
            redirectCount++
        }

        return response
    }

    private fun resolveRedirectUrl(currentUrl: String, location: String): String {
        return when {
            location.startsWith("http://") || location.startsWith("https://") -> location
            location.startsWith("/") -> {
                val protocol = if (currentUrl.startsWith("https://")) "https" else "http"
                val host = currentUrl.substringAfter("://").substringBefore("/")
                "$protocol://$host$location"
            }

            else -> {
                val base = currentUrl.substringBeforeLast("/", currentUrl)
                "$base/$location"
            }
        }
    }

    /**
     * 获取用户组信息（用于获取组编码）
     * 返回用户所属的打卡组，如"本科生"组的编码
     */
    suspend fun getUserGroups(): HttpResponse {
        return httpClient.get(USER_GROUPS_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 获取打卡任务列表
     * @param status 任务状态：1=待签到, 2=已完成, 3=已缺勤
     */
    suspend fun getTaskList(status: Int = 1): HttpResponse {
        return httpClient.get("$TASK_LIST_BASE_URL?status=$status") {
            header("Accept", "application/json, text/plain, */*")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 获取打卡任务列表（使用 Cookie）
     * @param status 任务状态：1=待签到, 2=已完成, 3=已缺勤
     * @param cookies Cookie 字符串
     * @param openId 微信 OpenID，用于 Referer 参数
     */
    suspend fun getTaskListWithCookies(
        status: Int = 1,
        cookies: String,
        openId: String? = null
    ): HttpResponse {
        val referer = if (openId != null) {
            "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        } else {
            "$QFHY_BASE/xg/app/qddk/admin/qddkdk"
        }
        return httpClient.get("$TASK_LIST_BASE_URL?status=$status") {
            header("Accept", "application/json, text/plain, */*")
            header("Cookie", cookies)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 执行签到
     * @param groupCode 用户组编码（如 "_sudy2_XDH6iuWnV4="）
     */
    suspend fun submitCheckin(groupCode: String): HttpResponse {
        return httpClient.post("$CHECKIN_BASE_URL/$groupCode/qddk/set.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
            header("Content-Length", "0")
        }
    }

    // ==================== 扫码登录相关 API ====================

    /**
     * 获取微信扫码登录的 ClientId
     * GET https://qfhy.suse.edu.cn/edu/v2/weixin/getClientId
     */
    suspend fun getWechatClientId(): HttpResponse {
        return httpClient.get("$QFHY_BASE/edu/v2/weixin/getClientId") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
        }
    }

    /**
     * 获取微信扫码登录的二维码 URL
     * POST https://qfhy.suse.edu.cn/edu/v2/weixin/getQrCodeUrl
     */
    suspend fun getWechatQrCodeUrl(appId: String, clientId: String): HttpResponse {
        return httpClient.post("$QFHY_BASE/edu/v2/weixin/getQrCodeUrl") {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            setBody("""{"app_id":"$appId","client_id":"$clientId"}""")
        }
    }

    /**
     * 处理微信扫码回调获取 Session
     * GET https://qfhy.suse.edu.cn/callback/edu/?ybClientId=...
     */
    suspend fun handleWechatCallback(callbackUrl: String): HttpResponse {
        return httpClient.get(callbackUrl) {
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
        }
    }

    /**
     * 检查微信扫码登录状态（轮询）
     * POST https://qfhy.suse.edu.cn/edu/v2/weixin/checkScan
     */
    suspend fun checkWechatScanStatus(clientId: String): HttpResponse {
        return httpClient.post("$QFHY_BASE/edu/v2/weixin/checkScan") {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            setBody("""{"client_id":"$clientId"}""")
        }
    }

    /**
     * 获取用户信息（使用 edu API）
     * GET https://qfhy.suse.edu.cn/edu/api/v1/user/getinfo
     */
    suspend fun getEduUserInfo(): HttpResponse {
        return httpClient.get("$QFHY_BASE/edu/api/v1/user/getinfo") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
        }
    }

    /**
     * 获取用户信息（使用自定义 Cookie）
     * GET https://qfhy.suse.edu.cn/edu/api/v1/user/getinfo
     * 用于 WebView 扫码登录后获取用户信息
     */
    suspend fun getEduUserInfoWithCookies(cookies: String): HttpResponse {
        return httpClient.get("$QFHY_BASE/edu/api/v1/user/getinfo") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Content-Type", "application/json")
            header("Cookie", cookies)
            header("Referer", "$QFHY_BASE/edu/admin/")
        }
    }

    /**
     * 获取用户当前信息（qfhy site API）
     * GET https://qfhy.suse.edu.cn/site/app/base/common/api/user/current.rst
     * 用于获取 652 签到所需的用户信息
     */
    suspend fun getUserCurrentWithCookies(cookies: String): HttpResponse {
        return httpClient.get("$QFHY_BASE/site/app/base/common/api/user/current.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", cookies)
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
        }
    }

    /**
     * 获取用户组身份（用于签到）
     * GET https://qfhy.suse.edu.cn/site/app/base/common/api/group/qddk/identity.rst
     *
     * 重要：根据 HAR 分析，Referer 必须包含 open_id 参数才能正常认证
     * 格式：https://qfhy.suse.edu.cn/xg/app/qddk/admin?open_id=xxx
     */
    suspend fun getGroupIdentityWithCookies(cookies: String, openId: String? = null): HttpResponse {
        val referer = if (openId != null) {
            "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        } else {
            "$QFHY_BASE/xg/app/qddk/admin/qddkdk"
        }
        return httpClient.get("$QFHY_BASE/site/app/base/common/api/group/qddk/identity.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", cookies)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 使用 ticket 完成 SSO 认证
     * 从 _sop_session_ JWT 中提取的 ticket 可以用来获取 SESSION cookie
     * GET https://qfhy.suse.edu.cn/site/appware/system/sso/login?ticket=xxx&target=xxx
     */
    suspend fun completeSsoWithTicket(ticket: String, sopSessionCookie: String): HttpResponse {
        val targetUrl = "$QFHY_BASE/xg/app/qddk/admin/qddkdk"
        return httpClient.get("$QFHY_BASE/site/appware/system/sso/login") {
            parameter("ticket", ticket)
            parameter("target", targetUrl)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sopSessionCookie)
            header("Referer", "$QFHY_BASE/edu/")
        }
    }

    /**
     * 访问 /xg/ 签到页面，触发 SESSION cookie 生成
     * 根据 HAR 分析，访问 /xg/app/qddk/admin?open_id=xxx 页面会自动设置 SESSION
     * 这是从 /edu/ 跳转到 /xg/ 时的关键步骤
     */
    suspend fun accessXgPage(sopSessionCookie: String, openId: String): HttpResponse {
        return httpClient.get("$QFHY_BASE/xg/app/qddk/admin") {
            parameter("open_id", openId)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sopSessionCookie)
            header("Referer", "$QFHY_BASE/edu/")
        }
    }

    /**
     * 访问 /site/user/current API 来初始化 SESSION
     * 这个 API 可能会自动创建 SESSION
     */
    suspend fun initSiteSession(sopSessionCookie: String, openId: String): HttpResponse {
        val referer = "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        return httpClient.get("$QFHY_BASE/site/app/base/common/api/user/current.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sopSessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 执行签到（使用 Cookie）
     * POST https://qfhy.suse.edu.cn/site/app/base/common/api/group/{groupCode}/qddk/set.rst
     *
     * 重要：根据 HAR 分析，Referer 必须包含 open_id 参数
     */
    suspend fun submitCheckinWithCookies(
        groupCode: String,
        cookies: String,
        openId: String? = null
    ): HttpResponse {
        val referer = if (openId != null) {
            "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        } else {
            "$QFHY_BASE/xg/app/qddk/admin/qddkdk"
        }
        return httpClient.post("$CHECKIN_BASE_URL/$groupCode/qddk/set.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Cookie", cookies)
            header("Referer", referer)
            header("appcode", "qddk")
            header("Content-Length", "0")
        }
    }

    // ==================== 基于位置的签到 API ====================

    /**
     * 获取签到任务详情（使用 HttpClient cookie storage，密码登录后使用）
     * GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId}
     */
    suspend fun getTaskDetail(taskId: Long): HttpResponse {
        return httpClient.get("$QFHY_BASE/site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId=$taskId") {
            header("Accept", "application/json, text/plain, */*")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 提交位置签到（使用 HttpClient cookie storage，密码登录后使用）
     * POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
     */
    suspend fun submitLocationCheckin(requestBody: String): HttpResponse {
        return httpClient.post("$QFHY_BASE/site/qddk/qdrw/api/checkSignLocationWithPhoto.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
            setBody(requestBody)
        }
    }

    /**
     * 获取签到任务详情（使用完整 Cookie 字符串）
     * GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId}
     */
    suspend fun getTaskDetailWithCookies(cookies: String, taskId: Long): HttpResponse {
        return httpClient.get("$QFHY_BASE/site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId=$taskId") {
            header("Accept", "application/json, text/plain, */*")
            header("Cookie", cookies)
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 提交位置签到（使用完整 Cookie 字符串）
     * POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
     */
    suspend fun submitLocationCheckinWithCookies(cookies: String, requestBody: String): HttpResponse {
        return httpClient.post("$QFHY_BASE/site/qddk/qdrw/api/checkSignLocationWithPhoto.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Cookie", cookies)
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
            setBody(requestBody)
        }
    }

    /**
     * 获取打卡任务列表（使用 Cookie）
     * GET /site/qddk/qdrw/api/myList.rst?status={status}
     * @param cookies 完整的 Cookie 字符串（包含 SESSION 和 _sop_session_）
     * @param status 任务状态：1=待签到（未打卡）, 2=已完成（已打卡）, 3=已缺勤
     */
    suspend fun getTaskListWithCookies(cookies: String, status: Int = 1): HttpResponse {
        return httpClient.get("$QFHY_BASE/site/qddk/qdrw/api/myList.rst?status=$status") {
            header("Accept", "application/json, text/plain, */*")
            header("Cookie", cookies)
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 获取待签到任务列表（未打卡）
     * 使用扫码登录的 Session 调用
     */
    suspend fun getPendingTasksWithSession(sessionToken: String): HttpResponse {
        return getTaskListWithCookies(sessionToken, status = 1)
    }

    /**
     * 获取已完成任务列表（已打卡）
     */
    suspend fun getCompletedTasksWithCookies(cookies: String): HttpResponse {
        return getTaskListWithCookies(cookies, status = 2)
    }

    /**
     * 获取已缺勤任务列表
     */
    suspend fun getAbsentTasksWithCookies(cookies: String): HttpResponse {
        return getTaskListWithCookies(cookies, status = 3)
    }

    /**
     * 获取签到任务详情
     * GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId}
     */
    suspend fun getTaskDetailWithSession(sessionToken: String, taskId: Long): HttpResponse {
        return httpClient.get("$QFHY_BASE/site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId=$taskId") {
            header("Accept", "application/json, text/plain, */*")
            header("Cookie", "SESSION=$sessionToken")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
        }
    }

    /**
     * 提交位置签到
     * POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
     */
    suspend fun submitLocationCheckin(
        sessionToken: String,
        requestBody: String
    ): HttpResponse {
        return httpClient.post("$QFHY_BASE/site/qddk/qdrw/api/checkSignLocationWithPhoto.rst") {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Cookie", "SESSION=$sessionToken")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            header("appcode", "qddk")
            setBody(requestBody)
        }
    }
}