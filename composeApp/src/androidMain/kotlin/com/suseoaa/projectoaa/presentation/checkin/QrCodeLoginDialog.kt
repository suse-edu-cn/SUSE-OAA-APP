package com.suseoaa.projectoaa.presentation.checkin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 美观的二维码登录对话框
 * 拦截 WebView 的二维码 API 请求，获取二维码 URL 显示在原生 UI 中
 * WebView 在后台继续运行以处理登录回调
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QrCodeLoginDialog(
    onLoginSuccess: (Map<String, String>) -> Unit,
    onLoginError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 状态：0=加载中, 1=显示二维码, 2=登录中, 3=成功, 4=失败
    var loadingState by remember { mutableIntStateOf(0) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    // 处理登录成功
    fun handleLoginSuccess() {
        loadingState = 2
        
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://qfhy.suse.edu.cn")
        
        println("[QrCode] 获取到的cookies: $cookies")
        
        if (!cookies.isNullOrBlank()) {
            val cookieMap = cookies.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { cookie ->
                    val parts = cookie.split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim()
                    else null
                }
                .toMap()
            
            if (cookieMap.containsKey("_sop_session_")) {
                loadingState = 3
                onLoginSuccess(cookieMap)
                return
            }
        }
        
        loadingState = 4
        errorMessage = "未能获取登录凭证"
    }
    
    // 刷新二维码
    fun refreshQrCode() {
        loadingState = 0
        qrCodeBitmap = null
        webView?.reload()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (loadingState) {
                        0 -> "正在加载二维码..."
                        1 -> "微信扫码登录"
                        2 -> "正在登录..."
                        3 -> "登录成功"
                        else -> "登录失败"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (loadingState) {
                    0 -> {
                        // 加载中
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "正在获取二维码...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    1 -> {
                        // 显示二维码
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = qrCodeBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "微信登录二维码",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                        
                        Text(
                            text = "请使用微信扫一扫\n扫描上方二维码登录",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedButton(onClick = { refreshQrCode() }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("刷新二维码")
                        }
                    }
                    
                    2 -> {
                        // 登录中
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("正在完成登录...", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    
                    3 -> {
                        // 成功
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓ 登录成功", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    
                    else -> {
                        // 失败
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(errorMessage ?: "未知错误", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Button(onClick = { refreshQrCode() }) { Text("重试") }
                        }
                    }
                }
                
                // WebView 用于处理登录回调 - 使用透明覆盖层
                // 不能完全隐藏，否则页面的轮询脚本可能不工作
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .background(Color.Transparent)
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                // 设置正常大小，让页面能正常运行
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                // 透明背景
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                
                                // JavaScript 接口 - 接收页面传来的二维码数据
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onQrCodeData(base64Img: String) {
                                        println("[QrCode] JS接口收到二维码: 长度=${base64Img.length}")
                                        if (base64Img.isNotBlank() && base64Img.length > 100) {
                                            try {
                                                val base64Data = base64Img.substringAfter("base64,", base64Img)
                                                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                if (bitmap != null) {
                                                    println("[QrCode] 解码成功: ${bitmap.width}x${bitmap.height}")
                                                    mainHandler.post {
                                                        qrCodeBitmap = bitmap
                                                        loadingState = 1
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                println("[QrCode] 解码失败: ${e.message}")
                                            }
                                        }
                                    }
                                }, "Android")
                                
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                                
                                webViewClient = object : WebViewClient() {
                                    
                                    // 在页面开始加载时注入拦截脚本
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        
                                        if (url?.contains("qrcodelogin") == true) {
                                            val interceptJs = """
                                                (function() {
                                                    if (window._qrIntercepted) return;
                                                    window._qrIntercepted = true;
                                                    
                                                    console.log('[QrCode] 注入拦截脚本 (onPageStarted)');
                                                    
                                                    // 拦截 fetch
                                                    var originalFetch = window.fetch;
                                                    window.fetch = function(url, options) {
                                                        return originalFetch.apply(this, arguments).then(function(response) {
                                                            if (url && url.toString().indexOf('getQrCodeUrl') !== -1) {
                                                                console.log('[QrCode] 拦截到 fetch 响应');
                                                                response.clone().json().then(function(data) {
                                                                    console.log('[QrCode] 响应数据');
                                                                    if (data && data.data && data.data.img) {
                                                                        console.log('[QrCode] 发送二维码到 Android');
                                                                        Android.onQrCodeData(data.data.img);
                                                                    }
                                                                }).catch(function(e) {
                                                                    console.log('[QrCode] 解析响应失败: ' + e);
                                                                });
                                                            }
                                                            return response;
                                                        });
                                                    };
                                                    
                                                    // 拦截 XMLHttpRequest
                                                    var originalXHR = window.XMLHttpRequest;
                                                    window.XMLHttpRequest = function() {
                                                        var xhr = new originalXHR();
                                                        var originalOpen = xhr.open;
                                                        var requestUrl = '';
                                                        
                                                        xhr.open = function(method, url) {
                                                            requestUrl = url;
                                                            return originalOpen.apply(this, arguments);
                                                        };
                                                        
                                                        xhr.addEventListener('load', function() {
                                                            if (requestUrl.indexOf('getQrCodeUrl') !== -1) {
                                                                console.log('[QrCode] 拦截到 XHR 响应');
                                                                try {
                                                                    var data = JSON.parse(xhr.responseText);
                                                                    if (data && data.data && data.data.img) {
                                                                        console.log('[QrCode] 发送二维码到 Android (XHR)');
                                                                        Android.onQrCodeData(data.data.img);
                                                                    }
                                                                } catch(e) {
                                                                    console.log('[QrCode] 解析 XHR 响应失败: ' + e);
                                                                }
                                                            }
                                                        });
                                                        
                                                        return xhr;
                                                    };
                                                    
                                                    console.log('[QrCode] 拦截脚本注入完成');
                                                })();
                                            """.trimIndent()
                                            
                                            view?.evaluateJavascript(interceptJs, null)
                                        }
                                    }
                                    
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        println("[QrCode] 页面加载完成: $url")
                                        
                                        // 检测登录成功 - 扫码后会跳转离开二维码页面
                                        if (url != null && !url.contains("qrcodelogin")) {
                                            // 只要离开二维码页面就检查 cookies
                                            if (url.contains("/callback/edu/") || 
                                                url.contains("/xg/app/") || 
                                                url.endsWith("/edu/") ||
                                                url.endsWith("/edu")) {
                                                println("[QrCode] 检测到登录成功，URL: $url")
                                                handleLoginSuccess()
                                            }
                                        }
                                    }
                                }
                                
                                loadUrl("https://qfhy.suse.edu.cn/edu/v1/wechat/qrcodelogin?appId=wx130c9f0196e29149&ybAppId=yszbOwOyvwBVkjP3&targetUrl=https%3A%2F%2Fqfhy.suse.edu.cn%2Fcallback%2Fedu%2F")
                            }
                        },
                        update = { view -> webView = view }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            webView?.removeJavascriptInterface("Android")
            webView?.destroy()
            webView = null
        }
    }
}
