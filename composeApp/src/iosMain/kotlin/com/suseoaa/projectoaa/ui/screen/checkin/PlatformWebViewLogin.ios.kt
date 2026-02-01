package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * iOS 平台的 WebView 扫码登录对话框实现
 * iOS 平台暂不支持 WebView 扫码登录
 */
@Composable
actual fun PlatformWebViewLoginDialog(
    onLoginSuccess: (cookies: Map<String, String>) -> Unit,
    onLoginError: (error: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("暂不支持") },
        text = { Text("iOS 平台暂不支持扫码登录，请使用密码登录添加账号。") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
