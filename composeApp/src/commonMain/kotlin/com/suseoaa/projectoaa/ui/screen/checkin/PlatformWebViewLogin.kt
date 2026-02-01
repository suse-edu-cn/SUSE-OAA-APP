package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.runtime.Composable

/**
 * 平台特定的 WebView 扫码登录对话框
 * 在 Android 上使用 WebView，在 iOS 上使用其他实现（暂不支持）
 */
@Composable
expect fun PlatformWebViewLoginDialog(
    onLoginSuccess: (cookies: Map<String, String>) -> Unit,
    onLoginError: (error: String) -> Unit,
    onDismiss: () -> Unit
)
