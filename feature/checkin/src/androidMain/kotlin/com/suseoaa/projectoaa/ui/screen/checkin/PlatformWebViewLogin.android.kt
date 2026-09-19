package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.runtime.Composable

/**
 * Android 平台的 WebView 扫码登录对话框实现
 */
@Composable
actual fun PlatformWebViewLoginDialog(
    onLoginSuccess: (cookies: Map<String, String>) -> Unit,
    onLoginError: (error: String) -> Unit,
    onDismiss: () -> Unit
) {
    QrCodeLoginDialog(
        onLoginSuccess = onLoginSuccess,
        onLoginError = onLoginError,
        onDismiss = onDismiss
    )
}

