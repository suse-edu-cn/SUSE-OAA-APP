package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * iOS 实现：iOS 没有系统返回键，所以为空实现
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS 没有系统返回键，不需要处理
}
