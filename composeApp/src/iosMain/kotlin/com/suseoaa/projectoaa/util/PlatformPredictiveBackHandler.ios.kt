package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformNavigationMode(): PlatformNavigationMode {
    return PlatformNavigationMode.ThreeButton
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (PlatformPredictiveBackEvent) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    // iOS 无系统预测返回手势回调，保持空实现。
}
