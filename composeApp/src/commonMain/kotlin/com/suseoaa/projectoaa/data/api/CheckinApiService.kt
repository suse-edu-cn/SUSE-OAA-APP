package com.suseoaa.projectoaa.data.api

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
 * 2. 获取用户组编码 (GET /site/app/base/common/api/user/groups.rst?appCode=qddk)
 * 3. 获取待签任务列表 (GET /site/qddk/qdrw/api/myList.rst?status=1)
 * 4. 执行打卡 (POST /site/app/base/common/api/group/{group_code}/qddk/set.rst)
 */
class CheckinApiService(private val httpClient: HttpClient) {

    companion object {
        private const val UIAS_BASE = "https://uias.suse.edu.cn"
        private const val QFHY_BASE = "https://qfhy.suse.edu.cn"

        // 登录服务地址
        private const val LOGIN_SERVICE =
            "$QFHY_BASE/site/appware/system/sso/login?target=$QFHY_BASE/xg/app/"
        private const val LOGIN_PAGE = "$UIAS_BASE/sso/login?service=$LOGIN_SERVICE"

        // 验证码地址
        private const val CAPTCHA_URL = "$UIAS_BASE/sso/captcha.jpg"

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
    suspend fun getLoginPage(): HttpResponse {
        return httpClient.get(LOGIN_PAGE) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
            )
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            )
            header("Accept-Language", "zh-CN,zh;q=0.9")
        }
    }

    /**
     * 获取验证码图片
     * @return 验证码图片的字节数组
     */
    suspend fun getCaptchaImage(): ByteArray {
        val response = httpClient.get(CAPTCHA_URL) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
            )
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
                append("service", LOGIN_SERVICE)
                append("username", username)
                append("password", encryptedPassword)
                append("authcode", captchaCode)
                append("execution", execution)
                append("encrypted", "true")
                append("_eventId", "submit")
                append("loginType", "1")
            }
        ) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
            )
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
    suspend fun followRedirect(redirectUrl: String): HttpResponse {
        return httpClient.get(redirectUrl)
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
}
