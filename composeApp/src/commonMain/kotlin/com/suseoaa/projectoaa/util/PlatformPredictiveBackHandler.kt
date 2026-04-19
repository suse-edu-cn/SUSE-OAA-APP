package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 系统导航模式。
 * Gesture: 全面屏手势导航
 * ThreeButton: 三键/非手势导航
 */
enum class PlatformNavigationMode {
    Gesture,
    ThreeButton
}

/**
 * 预测返回手势的边缘来源。
 */
enum class PlatformBackSwipeEdge {
    Left,
    Right
}

/**
 * 平台无关的预测返回事件。
 */
data class PlatformPredictiveBackEvent(
    val progress: Float,
    val swipeEdge: PlatformBackSwipeEdge,
    // 手指沿屏幕宽度的位移比例，范围 [0, 1]。
    // 在 Android 端由 touchX / screenWidth 计算；其他平台默认沿用 progress。
    val distanceProgress: Float = progress,
    // 手指在屏幕纵向位置的归一化比例，范围 [0, 1]，0 表示顶部，1 表示底部。
    val verticalPosition: Float = 0.5f
)

/**
 * 获取当前平台导航模式。
 */
@Composable
expect fun rememberPlatformNavigationMode(): PlatformNavigationMode

/**
 * 平台无关的预测返回手势处理器。
 */
@Composable
expect fun PlatformPredictiveBackHandler(
    enabled: Boolean = true,
    onProgress: (PlatformPredictiveBackEvent) -> Unit,
    onCancel: () -> Unit = {},
    onBack: () -> Unit
)
