package com.suseoaa.projectoaa.shared.data.remote

/**
 * 全项目所有远端地址的唯一来源。
 *
 * 之前 baseUrl 以字符串字面量散落在 ApiService、Repository、Screen、ViewModel
 * 里（同一个 `https://qfhy.suse.edu.cn` 在 5 个文件中各写一份），换域名或加代理
 * 时必须全局搜索替换。这里统一收口，任何新增接口都应从这里取前缀。
 */
object ApiConfig {

    // ==================== OAA 自建后端 ====================
    const val OAA_BASE = "https://api.suseoaa.com"

    // ==================== 教务系统 ====================
    const val SCHOOL_BASE = "https://jwgl.suse.edu.cn"
    const val SCHOOL_LOGIN_PAGE = "$SCHOOL_BASE/xtgl/login_slogin.html"

    // ==================== 统一身份认证 ====================
    const val UIAS_BASE = "https://uias.suse.edu.cn"

    // ==================== 青工护研（签到） ====================
    const val QFHY_BASE = "https://qfhy.suse.edu.cn"

    /** 微信扫码登录入口，Android 端两个 WebView 组件共用。 */
    const val QFHY_WECHAT_QR_LOGIN =
        "$QFHY_BASE/edu/v1/wechat/qrcodelogin" +
            "?appId=wx130c9f0196e29149" +
            "&ybAppId=yszbOwOyvwBVkjP3" +
            "&targetUrl=https%3A%2F%2Fqfhy.suse.edu.cn%2Fcallback%2Fedu%2F"

    /** 扫码登录成功后要跳转的管理页，[openId] 由回调带回。 */
    fun qfhyCheckinAdminUrl(openId: String) = "$QFHY_BASE/xg/app/qddk/admin?open_id=$openId"

    /** SSO 换取会话的地址，[encodedService] 为已 URL 编码的目标地址。 */
    fun qfhySsoLoginUrl(encodedService: String) =
        "$QFHY_BASE/site/appware/system/sso/loginUrl?service=$encodedService"

    // ==================== 应用更新 ====================
    /** 发版仓库坐标，Release 元数据与下载直链都由它拼出来。 */
    const val UPDATE_REPO_OWNER = "HuangZhuoRui"
    const val UPDATE_REPO_NAME = "SUSE-OAA-APP"

    /**
     * Release 元数据（检查更新、历史版本）一律走 GitHub 官方 API，
     * 代理服务器只承担下载加速这一件事。
     */
    const val GITHUB_API_BASE = "https://api.github.com"
    const val UPDATE_RELEASES = "$GITHUB_API_BASE/repos/$UPDATE_REPO_OWNER/$UPDATE_REPO_NAME/releases"
    const val UPDATE_LATEST_RELEASE = "$UPDATE_RELEASES/latest"

    /** GitHub 直链前缀，配合 [UPDATE_DOWNLOAD_PREFIX] 做下载加速替换。 */
    const val GITHUB_DOWNLOAD_PREFIX = "https://github.com/"
    const val UPDATE_RELEASE_PAGE = "$GITHUB_DOWNLOAD_PREFIX$UPDATE_REPO_OWNER/$UPDATE_REPO_NAME/releases"

    /** 下载加速代理，仅用于替换 APK 直链前缀。 */
    const val UPDATE_PROXY_BASE = "https://update.vincenthzr.org:8443"
    const val UPDATE_DOWNLOAD_PREFIX = "$UPDATE_PROXY_BASE/download/"

    // ==================== 端侧模型 ====================
    const val MODEL_HOST = "https://huggingface.co"
}
