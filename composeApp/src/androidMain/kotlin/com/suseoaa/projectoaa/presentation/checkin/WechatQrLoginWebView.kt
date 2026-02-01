package com.suseoaa.projectoaa.presentation.checkin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView 扫码登录的状态
 */
sealed class WebViewLoginState {
    /** 初始状态，准备访问首页获取 SESSION */
    data object PreparingSession : WebViewLoginState()
    data object Loading : WebViewLoginState()
    data object ShowingQrCode : WebViewLoginState()
    data object Scanned : WebViewLoginState()
    /** 已获取 _sop_session_，正在跳转到 /xg/ 页面获取 SESSION cookie */
    data class GettingSession(val sopSession: String, val openId: String) : WebViewLoginState()
    data class Success(val sessionCookies: Map<String, String>) : WebViewLoginState()
    data class Error(val message: String) : WebViewLoginState()
}

/**
 * 微信扫码登录成功后的用户信息
 */
data class WechatLoginResult(
    val studentId: String,
    val name: String,
    val sessionCookies: Map<String, String>
)

/**
 * 微信扫码登录 WebView 组件
 * 
 * 加载微信扫码登录页面，用户使用微信扫码后，
 * WebView 会自动处理回调并获取 Session。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WechatQrLoginWebView(
    onLoginSuccess: (WechatLoginResult) -> Unit,
    onLoginError: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loginState by remember { mutableStateOf<WebViewLoginState>(WebViewLoginState.PreparingSession) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var sessionPrepared by remember { mutableStateOf(false) }
    
    // 首页 URL - 用于获取 SESSION cookie
    val homeUrl = "https://qfhy.suse.edu.cn/"
    
    // 登录 URL - 直接使用微信二维码登录入口
    // 扫码后会回调到 targetUrl，同时设置 _sop_session_ cookie
    // 注意：使用 /edu/ 回调而不是 /xg/app/qddk，因为后者需要额外的 SSO 认证
    val loginUrl = "https://qfhy.suse.edu.cn/edu/v1/wechat/qrcodelogin?appId=wx130c9f0196e29149&ybAppId=yszbOwOyvwBVkjP3&targetUrl=https%3A%2F%2Fqfhy.suse.edu.cn%2Fcallback%2Fedu%2F"
    
    // 当登录成功时，自动调用回调
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is WebViewLoginState.Success -> {
                val sessionCookie = state.sessionCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                println("[WebView] 登录成功，自动获取用户信息...")
                println("[WebView] Session Cookie: $sessionCookie")
                
                // 直接回调成功，让 ViewModel 处理后续逻辑
                onLoginSuccess(WechatLoginResult(
                    studentId = "", // 需要通过 API 获取
                    name = "",
                    sessionCookies = state.sessionCookies
                ))
            }
            else -> {}
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = when (loginState) {
                    is WebViewLoginState.PreparingSession -> "正在准备..."
                    is WebViewLoginState.Loading -> "正在加载二维码..."
                    is WebViewLoginState.ShowingQrCode -> "请使用微信扫码登录"
                    is WebViewLoginState.Scanned -> "已扫码，请在微信中确认"
                    is WebViewLoginState.GettingSession -> "正在完成登录..."
                    is WebViewLoginState.Success -> "登录成功，正在获取用户信息..."
                    is WebViewLoginState.Error -> "登录失败"
                },
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // WebView 隐藏在后台运行
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                @Suppress("DEPRECATION")
                                databaseEnabled = true
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }
                            
                            // 启用 Cookie
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            
                            // 清除旧的 _sop_session_ cookie
                            val existingCookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
                            if (existingCookies?.contains("_sop_session_") == true) {
                                cookieManager.setCookie("https://qfhy.suse.edu.cn", "_sop_session_=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                                cookieManager.flush()
                                println("[WebView] 已清除旧的 _sop_session_ Cookie")
                            }
                            
                            // 添加 WebChromeClient 以支持 JS
                            webChromeClient = android.webkit.WebChromeClient()
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    println("[WebView] 开始加载: $url")
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    println("[WebView] 加载完成: $url")
                                    
                                    url?.let { currentUrl ->
                                        // 首页加载完成 - 检查是否获取到 SESSION，然后跳转到登录页
                                        if (loginState is WebViewLoginState.PreparingSession && 
                                            (currentUrl == "https://qfhy.suse.edu.cn/" || 
                                             currentUrl.startsWith("https://qfhy.suse.edu.cn/#"))) {
                                            println("[WebView] 首页加载完成，检查 SESSION cookie...")
                                            
                                            val cookieManager = CookieManager.getInstance()
                                            val cookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
                                            val hasSession = cookies?.contains("SESSION=") == true
                                            
                                            println("[WebView] 首页 Cookie: $cookies")
                                            println("[WebView] 首页已有 SESSION: $hasSession")
                                            
                                            // 不管是否有 SESSION，都继续加载登录页面
                                            // SESSION 可能会在后续请求中被创建
                                            sessionPrepared = true
                                            loginState = WebViewLoginState.Loading
                                            view?.postDelayed({
                                                println("[WebView] 开始加载登录页面...")
                                                view.loadUrl(loginUrl)
                                            }, 200)
                                            return
                                        }
                                        
                                        // 二维码页面（带 targetUrl）
                                        if (currentUrl.contains("qrcodelogin") && currentUrl.contains("targetUrl=")) {
                                            loginState = WebViewLoginState.ShowingQrCode
                                            return
                                        }
                                        
                                        // 二维码页面（没有 targetUrl）- 需要重新加载正确的 URL
                                        if (currentUrl.contains("qrcodelogin") && !currentUrl.contains("targetUrl=")) {
                                            println("[WebView] 检测到没有 targetUrl 的二维码页面，重新加载正确的 URL")
                                            view?.loadUrl(loginUrl)
                                            return
                                        }
                                        
                                        // 检测到 /callback/edu/ 回调或 /edu/ 页面 - 说明扫码成功
                                        if (currentUrl.contains("/callback/edu/") || 
                                            (currentUrl.contains("/edu/") && !currentUrl.contains("qrcodelogin"))) {
                                            println("[WebView] 检测到登录成功回调页面，检查 Cookie")
                                            loginState = WebViewLoginState.Scanned
                                            view?.postDelayed({
                                                handleLoginSuccessWithRetry(view, 0)
                                            }, 500)
                                            return
                                        }
                                        
                                        // 检测到 ybClientId 参数，说明扫码成功
                                        if (currentUrl.contains("ybClientId=") && currentUrl.contains("qfhy.suse.edu.cn")) {
                                            println("[WebView] 检测到扫码回调 (ybClientId)，检查 Cookie")
                                            loginState = WebViewLoginState.Scanned
                                            view?.postDelayed({
                                                handleLoginSuccessWithRetry(view, 0)
                                            }, 300)
                                            return
                                        }
                                        
                                        // 检测到认证相关页面（sudytech）
                                        if (currentUrl.contains("sudytech") && currentUrl.contains("ybClientId=")) {
                                            println("[WebView] 检测到 yiban 认证页面，标记为已扫码")
                                            loginState = WebViewLoginState.Scanned
                                            return
                                        }
                                        
                                        // SSO 登录页面 - 说明需要额外认证
                                        if (currentUrl.contains("/site/appware/system/sso/")) {
                                            println("[WebView] 检测到 SSO 登录页面，检查是否已有 _sop_session_")
                                            val cookieManager = CookieManager.getInstance()
                                            val cookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
                                            if (cookies?.contains("_sop_session_=") == true) {
                                                println("[WebView] 已有 _sop_session_，直接登录成功")
                                                handleLoginSuccessWithRetry(view, 0)
                                            }
                                            return
                                        }
                                        
                                        // 打卡页面 - 如果正在获取SESSION状态，检查cookie
                                        if (currentUrl.contains("/xg/app/qddk")) {
                                            println("[WebView] 检测到 /xg/ 打卡页面")
                                            
                                            // 如果是 GettingSession 状态，检查是否获取到了 SESSION
                                            if (loginState is WebViewLoginState.GettingSession) {
                                                println("[WebView] 正在获取 SESSION，延迟检查 Cookie...")
                                                view?.postDelayed({
                                                    handleSessionAcquisition(view)
                                                }, 1000) // 等待 1 秒让 cookie 完全设置
                                            } else if (!currentUrl.contains("sso/loginUrl") &&
                                                !currentUrl.contains("qrcodelogin")) {
                                                println("[WebView] 检测到打卡页面，检查 Cookie 状态")
                                                view?.postDelayed({
                                                    handleLoginSuccessWithRetry(view, 0)
                                                }, 500)
                                            }
                                        }
                                    }
                                }
                                
                                /**
                                 * 处理 SESSION 获取
                                 * 在跳转到 /xg/ 页面后调用
                                 */
                                private fun handleSessionAcquisition(view: WebView?) {
                                    val cookieManager = CookieManager.getInstance()
                                    val cookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
                                    println("[WebView] /xg/ 页面 Cookie: $cookies")
                                    
                                    if (cookies.isNullOrBlank()) {
                                        println("[WebView] Cookie 为空，登录失败")
                                        loginState = WebViewLoginState.Error("无法获取 Cookie")
                                        return
                                    }
                                    
                                    // 解析 Cookie
                                    val cookieMap = cookies.split(";")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .mapNotNull { cookie ->
                                            val parts = cookie.split("=", limit = 2)
                                            if (parts.size == 2) {
                                                parts[0].trim() to parts[1].trim()
                                            } else null
                                        }
                                        .toMap()
                                    
                                    val hasSession = cookieMap.containsKey("SESSION")
                                    val hasSopSession = cookieMap.containsKey("_sop_session_")
                                    
                                    println("[WebView] /xg/ 页面 Cookie 解析: SESSION=$hasSession, _sop_session_=$hasSopSession")
                                    println("[WebView] Cookie keys: ${cookieMap.keys}")
                                    
                                    if (hasSession || hasSopSession) {
                                        println("[WebView] 成功获取到 Cookie! SESSION=$hasSession, _sop_session_=$hasSopSession")
                                        loginState = WebViewLoginState.Success(cookieMap)
                                    } else {
                                        println("[WebView] 未获取到有效 Cookie")
                                        // 如果没有获取到，再重试一次
                                        view?.postDelayed({
                                            handleLoginSuccessWithRetry(view, 0)
                                        }, 500)
                                    }
                                }
                                
                                private fun handleLoginSuccessWithRetry(view: WebView?, retryCount: Int) {
                                    if (retryCount > 10) {
                                        println("[WebView] 重试超时，登录失败")
                                        loginState = WebViewLoginState.Error("登录超时，请重试")
                                        return
                                    }
                                    
                                    // 防止重复调用（但允许 GettingSession 状态继续处理）
                                    if (loginState is WebViewLoginState.Success) {
                                        println("[WebView] 已经是成功状态，跳过")
                                        return
                                    }
                                    
                                    // 获取 Cookie
                                    val cookieManager = CookieManager.getInstance()
                                    val cookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
                                    println("[WebView] 获取到 Cookies (第 ${retryCount + 1} 次): $cookies")
                                    
                                    if (cookies.isNullOrBlank()) {
                                        println("[WebView] Cookie 为空，500ms 后重试")
                                        view?.postDelayed({ handleLoginSuccessWithRetry(view, retryCount + 1) }, 500)
                                        return
                                    }
                                    
                                    // 解析 Cookie
                                    val cookieMap = cookies.split(";")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .mapNotNull { cookie ->
                                            val parts = cookie.split("=", limit = 2)
                                            if (parts.size == 2) {
                                                parts[0].trim() to parts[1].trim()
                                            } else null
                                        }
                                        .toMap()
                                    
                                    // 检查是否同时有 SESSION 和 _sop_session_
                                    val hasSession = cookieMap.containsKey("SESSION")
                                    val hasSopSession = cookieMap.containsKey("_sop_session_")
                                    
                                    println("[WebView] Cookie 状态: SESSION=$hasSession, _sop_session_=$hasSopSession")
                                    
                                    // 如果同时有 SESSION 和 _sop_session_，直接登录成功
                                    if (hasSession && hasSopSession) {
                                        println("[WebView] 登录成功! (同时有 SESSION 和 _sop_session_)")
                                        loginState = WebViewLoginState.Success(cookieMap)
                                        return
                                    }
                                    
                                    // 如果只有 _sop_session_，需要跳转到 /xg/ 页面获取 SESSION
                                    if (hasSopSession && !hasSession) {
                                        val sopSession = cookieMap["_sop_session_"] ?: ""
                                        
                                        // 从 _sop_session_ JWT 中提取 openId
                                        val openId = extractOpenIdFromSopSession(sopSession)
                                        
                                        if (openId != null) {
                                            // 检查是否已经在获取SESSION的状态，避免无限循环
                                            if (loginState is WebViewLoginState.GettingSession) {
                                                println("[WebView] 已经在获取 SESSION 状态，继续等待...")
                                                view?.postDelayed({ handleLoginSuccessWithRetry(view, retryCount + 1) }, 500)
                                                return
                                            }
                                            
                                            println("[WebView] 检测到 _sop_session_，但没有 SESSION")
                                            println("[WebView] 提取到 openId: $openId")
                                            println("[WebView] 调用 SSO loginUrl API 获取 SESSION...")
                                            
                                            loginState = WebViewLoginState.GettingSession(sopSession, openId)
                                            
                                            // 调用 SSO loginUrl 接口获取 SESSION cookie
                                            // 根据HAR分析，这个接口会返回 Set-Cookie: SESSION
                                            val targetUrl = "https://qfhy.suse.edu.cn/xg/app/qddk/admin?open_id=$openId"
                                            val encodedTarget = java.net.URLEncoder.encode(targetUrl, "UTF-8")
                                            val ssoUrl = "https://qfhy.suse.edu.cn/site/appware/system/sso/loginUrl?service=$encodedTarget"
                                            view?.loadUrl(ssoUrl)
                                            return
                                        } else {
                                            println("[WebView] 无法从 _sop_session_ 中提取 openId，直接使用现有 cookie")
                                            loginState = WebViewLoginState.Success(cookieMap)
                                            return
                                        }
                                    }
                                    
                                    // 如果只有 SESSION（没有 _sop_session_），也接受
                                    if (hasSession) {
                                        println("[WebView] 登录成功! (使用 SESSION)")
                                        loginState = WebViewLoginState.Success(cookieMap)
                                        return
                                    }
                                    
                                    println("[WebView] 没有有效的登录 Cookie，500ms 后重试")
                                    view?.postDelayed({ handleLoginSuccessWithRetry(view, retryCount + 1) }, 500)
                                }
                                
                                /**
                                 * 从 _sop_session_ JWT 中提取 openId
                                 * JWT payload 示例:
                                 * {
                                 *   "uid": "23341010304",
                                 *   "ticket": "xxx",
                                 *   "extra": "{\"groupName\":\"\",\"identityType\":1,\"openId\":\"oXL_x6lMwe35D-T6qoiRM8_SErJA\",...}"
                                 * }
                                 * 注意: openId 在 extra 字段内，extra 是嵌套的 JSON 字符串（带转义）
                                 */
                                private fun extractOpenIdFromSopSession(sopSession: String): String? {
                                    return try {
                                        // JWT 格式: header.payload.signature
                                        val parts = sopSession.split(".")
                                        if (parts.size < 2) {
                                            println("[WebView] JWT 格式错误，parts.size=${parts.size}")
                                            return null
                                        }
                                        
                                        val payload = parts[1]
                                        // Base64 URL-safe 解码，添加 padding
                                        val paddedPayload = when (payload.length % 4) {
                                            2 -> payload + "=="
                                            3 -> payload + "="
                                            else -> payload
                                        }
                                        val decodedBytes = android.util.Base64.decode(paddedPayload, android.util.Base64.URL_SAFE)
                                        val decodedPayload = String(decodedBytes, Charsets.UTF_8)
                                        println("[WebView] JWT payload: $decodedPayload")
                                        
                                        // 使用 JSONObject 来正确解析 JSON
                                        val payloadJson = org.json.JSONObject(decodedPayload)
                                        
                                        // extra 字段是一个 JSON 字符串
                                        val extraString = payloadJson.optString("extra", "")
                                        if (extraString.isBlank()) {
                                            println("[WebView] extra 字段为空")
                                            return null
                                        }
                                        
                                        // 解析 extra JSON 字符串
                                        val extraJson = org.json.JSONObject(extraString)
                                        val openId = extraJson.optString("openId", "")
                                        
                                        if (openId.isBlank()) {
                                            println("[WebView] openId 为空")
                                            return null
                                        }
                                        
                                        println("[WebView] 从 extra 中提取到 openId: $openId")
                                        openId
                                    } catch (e: Exception) {
                                        println("[WebView] 解析 _sop_session_ 失败: ${e.message}")
                                        e.printStackTrace()
                                        null
                                    }
                                }
                                
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    println("[WebView] 拦截 URL: $url")
                                    
                                    // 检测到 ybClientId 参数，说明扫码成功，正在进行认证
                                    if (url.contains("ybClientId=")) {
                                        println("[WebView] 检测到认证回调，继续加载...")
                                        loginState = WebViewLoginState.Scanned
                                        return false // 不拦截，让认证流程继续
                                    }
                                    
                                    // 允许以下域名的请求通过：
                                    // - qfhy.suse.edu.cn (主站)
                                    // - yibanng.sudytech.cn (认证中转)
                                    // - open.weixin.qq.com (微信授权)
                                    val allowedDomains = listOf(
                                        "qfhy.suse.edu.cn",
                                        "yibanng.sudytech.cn",
                                        "sudytech.cn",
                                        "open.weixin.qq.com",
                                        "weixin.qq.com"
                                    )
                                    
                                    return if (allowedDomains.any { url.contains(it) }) {
                                        false // 不拦截
                                    } else {
                                        println("[WebView] 拦截非允许域名: $url")
                                        true // 拦截其他域名的请求
                                    }
                                }
                                
                                @SuppressLint("WebViewClientOnReceivedSslError")
                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    // 忽略 SSL 错误（仅用于开发）
                                    handler?.proceed()
                                }
                                
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        loginState = WebViewLoginState.Error("网络错误: ${error?.description}")
                                    }
                                }
                            }
                            
                            // 先加载首页获取 SESSION cookie，然后再加载登录页面
                            println("[WebView] 开始加载首页获取 SESSION cookie...")
                            loadUrl(homeUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 根据状态显示覆盖层
                when (val state = loginState) {
                    is WebViewLoginState.PreparingSession,
                    is WebViewLoginState.Loading -> {
                        // 准备和加载时显示遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Text(
                                    text = when (loginState) {
                                        is WebViewLoginState.PreparingSession -> "正在准备登录环境..."
                                        is WebViewLoginState.Loading -> "正在加载二维码..."
                                        else -> "处理中..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    is WebViewLoginState.ShowingQrCode -> {
                        // 显示二维码时不遮挡WebView，只在底部显示提示
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 底部提示条
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "请使用微信扫描上方二维码",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "扫码后在微信中确认授权即可完成登录",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    is WebViewLoginState.Scanned,
                    is WebViewLoginState.GettingSession,
                    is WebViewLoginState.Success -> {
                        // 扫码后显示遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Text(
                                    text = when (loginState) {
                                        is WebViewLoginState.Scanned -> "已扫码，请在微信中确认授权"
                                        is WebViewLoginState.GettingSession -> "正在完成登录流程..."
                                        is WebViewLoginState.Success -> "登录成功！正在获取用户信息..."
                                        else -> "处理中..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    is WebViewLoginState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = state.message, 
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = { 
                                    loginState = WebViewLoginState.Loading
                                    webView?.loadUrl(loginUrl)
                                }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // 成功时自动处理，不需要手动按钮
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 清理 WebView
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                clearHistory()
                clearCache(true)
                destroy()
            }
        }
    }
}

