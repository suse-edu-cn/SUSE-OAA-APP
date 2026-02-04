package com.suseoaa.projectoaa.data.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

/**
 * 扫码签到 API 服务
 * 
 * 完整流程（基于HAR文件分析 - 2025年验证）：
 * 1. 获取 ClientId: GET /edu/v2/weixin/getClientId
 * 2. 获取二维码: POST /edu/v2/weixin/getQrCodeUrl
 * 3. 轮询扫码状态: POST /edu/v2/weixin/checkScan
 * 4. 扫码成功后获取 _sop_session_ JWT Cookie (通过 callback URL)
 * 5. ★关键★ 调用 SSO API 获取 SESSION: GET /site/appware/system/sso/loginUrl?service=<redirect_url>
 * 6. 获取用户信息: GET /site/app/base/common/api/user/current.rst
 * 7. 获取任务列表: GET /site/qddk/qdrw/api/myList.rst?status=1|2|3
 * 8. 获取任务详情: GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId=xxx
 * 9. 执行签到: POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
 * 
 * 关键 Cookie:
 * - _sop_session_: JWT，包含 uid(学号), ticket, extra(openId, userName等)
 * - SESSION: 由 SSO API 设置，用于 /site/ API 认证
 * 
 * 重要发现：SESSION 不是访问任何页面自动创建的，而是通过 SSO API 专门设置！
 */
class QrCodeCheckinApiService(private val httpClient: HttpClient) {

    companion object {
        private const val QFHY_BASE = "https://qfhy.suse.edu.cn"
        
        // 微信扫码登录相关
        private const val CLIENT_ID_URL = "$QFHY_BASE/edu/v2/weixin/getClientId"
        private const val QR_CODE_URL = "$QFHY_BASE/edu/v2/weixin/getQrCodeUrl"
        private const val CHECK_SCAN_URL = "$QFHY_BASE/edu/v2/weixin/checkScan"
        
        // ★关键★ SSO API - 用于获取 SESSION
        private const val SSO_LOGIN_URL = "$QFHY_BASE/site/appware/system/sso/loginUrl"
        
        // 签到相关
        private const val USER_CURRENT_URL = "$QFHY_BASE/site/app/base/common/api/user/current.rst"
        private const val GROUP_IDENTITY_URL = "$QFHY_BASE/site/app/base/common/api/group/qddk/identity.rst"
        private const val TASK_LIST_URL = "$QFHY_BASE/site/qddk/qdrw/api/myList.rst"
        private const val TASK_DETAIL_URL = "$QFHY_BASE/site/qddk/qdrw/qdxx/api/detailList.rst"
        private const val CHECKIN_URL = "$QFHY_BASE/site/qddk/qdrw/api/checkSignLocationWithPhoto.rst"
        
        // 微信 AppId (从 HAR 获取)
        const val WECHAT_APP_ID = "wx130c9f0196e29149"
    }

    // ==================== 扫码登录流程 ====================

    /**
     * 步骤1: 获取微信扫码登录的 ClientId
     * GET /edu/v2/weixin/getClientId
     * 
     * 响应示例:
     * {"code":200,"msg":"success","data":{"clientId":"xxx"}}
     */
    suspend fun getClientId(): HttpResponse {
        return httpClient.get(CLIENT_ID_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
        }
    }

    /**
     * 步骤2: 获取微信扫码二维码
     * POST /edu/v2/weixin/getQrCodeUrl
     * 
     * 请求体: {"app_id":"wx130c9f0196e29149","client_id":"xxx"}
     * 响应示例: {"code":200,"data":{"img":"base64编码的图片","url":"..."}}
     */
    suspend fun getQrCodeUrl(clientId: String): HttpResponse {
        return httpClient.post(QR_CODE_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            setBody("""{"app_id":"$WECHAT_APP_ID","client_id":"$clientId"}""")
        }
    }

    /**
     * 步骤3: 轮询检查扫码状态
     * POST /edu/v2/weixin/checkScan
     * 
     * 请求体: {"client_id":"xxx"}
     * 响应示例: 
     * - 等待扫码: {"code":200,"data":{"status":0}}
     * - 已扫码待确认: {"code":200,"data":{"status":1}}
     * - 已确认: {"code":200,"data":{"status":2,"callbackUrl":"..."}}
     */
    suspend fun checkScanStatus(clientId: String): HttpResponse {
        return httpClient.post(CHECK_SCAN_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin/qddkdk")
            setBody("""{"client_id":"$clientId"}""")
        }
    }

    /**
     * 步骤4: 处理扫码回调获取 _sop_session_ Cookie
     * GET callback URL (从 checkScan 返回)
     * 
     * 回调会设置 _sop_session_ JWT Cookie，包含:
     * - uid: 学号
     * - ticket: SSO票据
     * - extra: {"openId":"xxx","userName":"xxx",...}
     */
    suspend fun handleCallback(callbackUrl: String): HttpResponse {
        return httpClient.get(callbackUrl) {
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
        }
    }

    /**
     * ★步骤5★ 调用 SSO API 获取 SESSION Cookie
     * GET /site/appware/system/sso/loginUrl?service=<redirect_url>
     * 
     * 这是关键步骤！SESSION Cookie 是通过此 API 设置的，而不是访问其他页面自动获取。
     * 
     * 必须携带 _sop_session_ Cookie
     * 响应会设置 SESSION Cookie，用于后续 /site/ API 调用
     * 
     * @param sopSessionCookie _sop_session_ Cookie 字符串 (格式: "_sop_session_=xxxxx")
     * @param openId 用户的微信 OpenId (用于构建 redirect URL)
     */
    suspend fun getSsoSession(sopSessionCookie: String, openId: String): HttpResponse {
        // service 参数需要 URL 编码
        val serviceUrl = "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        val encodedService = serviceUrl.encodeURLParameter()
        
        return httpClient.get("$SSO_LOGIN_URL?service=$encodedService") {
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sopSessionCookie)
            header("Referer", "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        }
    }

    /**
     * 步骤5 (旧方法，保留兼容): 访问签到页面获取 SESSION Cookie
     * 注意：此方法已不推荐使用，请使用 getSsoSession()
     */
    @Deprecated("使用 getSsoSession() 代替", replaceWith = ReplaceWith("getSsoSession(sopSessionCookie, openId)"))
    suspend fun accessCheckinPage(sopSessionCookie: String, openId: String): HttpResponse {
        return httpClient.get("$QFHY_BASE/xg/app/qddk/admin") {
            parameter("open_id", openId)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sopSessionCookie)
            header("Referer", "$QFHY_BASE/edu/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        }
    }

    // ==================== 签到功能 ====================

    /**
     * 获取当前用户信息
     * GET /site/app/base/common/api/user/current.rst
     * 
     * 需要 SESSION Cookie
     */
    suspend fun getCurrentUser(sessionCookie: String, openId: String? = null): HttpResponse {
        val referer = buildReferer(openId)
        return httpClient.get(USER_CURRENT_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 获取用户信息 (别名方法，供 Repository 调用)
     */
    suspend fun getUserInfo(cookies: String): HttpResponse = getCurrentUser(cookies)

    /**
     * 获取用户组身份 (groupCode)
     * GET /site/app/base/common/api/group/qddk/identity.rst
     * 
     * 需要 SESSION Cookie
     * Referer 必须包含 open_id 参数
     */
    suspend fun getGroupIdentity(sessionCookie: String, openId: String? = null): HttpResponse {
        val referer = buildReferer(openId)
        return httpClient.get(GROUP_IDENTITY_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 获取打卡任务列表
     * GET /site/qddk/qdrw/api/myList.rst?status={status}
     * 
     * @param status 任务状态: 1=待签到(未打卡), 2=已完成(已打卡), 3=已缺勤
     */
    suspend fun getTaskList(sessionCookie: String, status: Int = 1, openId: String? = null): HttpResponse {
        val referer = buildReferer(openId)
        return httpClient.get("$TASK_LIST_URL?status=$status") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 获取签到任务详情
     * GET /site/qddk/qdrw/qdxx/api/detailList.rst?qdrwId={taskId}
     * 
     * 返回当前签到状态、签到ID等信息
     */
    suspend fun getTaskDetail(sessionCookie: String, taskId: Long, openId: String? = null): HttpResponse {
        val referer = buildReferer(openId)
        return httpClient.get("$TASK_DETAIL_URL?qdrwId=$taskId") {
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
        }
    }

    /**
     * 执行定位签到
     * POST /site/qddk/qdrw/api/checkSignLocationWithPhoto.rst
     * 
     * 请求体示例:
     * {
     *   "id": 签到记录ID (从 detailList 获取的 dkxx.id),
     *   "qdzt": 1,
     *   "qdsj": "2025-09-08 20:33:02",
     *   "isOuted": 0,
     *   "isLated": 0,
     *   "dkddPhoto": "",
     *   "qdddjtdz": "四川省宜宾市...",
     *   "location": "{\"point\":[104.401341,28.482517],\"address\":\"...\"}",
     *   "txxx": "{}"
     * }
     */
    suspend fun submitCheckin(sessionCookie: String, requestBody: String, openId: String? = null): HttpResponse {
        val referer = buildReferer(openId)
        return httpClient.post(CHECKIN_URL) {
            header("Accept", "application/json, text/plain, */*")
            header("Content-Type", "application/json")
            header("Accept-Language", "zh-CN,zh;q=0.9")
            header("Cookie", sessionCookie)
            header("Referer", referer)
            header("appcode", "qddk")
            setBody(requestBody)
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建 Referer URL
     * 带 open_id 参数的 Referer 对于 /site/ API 认证很重要
     */
    private fun buildReferer(openId: String?): String {
        return if (openId != null) {
            "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"
        } else {
            "$QFHY_BASE/xg/app/qddk/admin/qddkdk"
        }
    }

    /**
     * 获取 HttpClient (供外部使用，如手动跟随重定向)
     */
    fun getHttpClient(): HttpClient = httpClient
}
