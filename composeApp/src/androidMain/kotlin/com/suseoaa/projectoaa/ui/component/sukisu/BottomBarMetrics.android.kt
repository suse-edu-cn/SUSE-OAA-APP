package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android 端保持原有观感：系统导航栏之上再留 24dp，让悬浮栏与屏幕底边拉开距离。
 */
actual object BottomBarMetrics {
    actual val outerTopPadding: Dp = 12.dp
    actual val outerBottomPadding: Dp = 24.dp
    actual val barHeight: Dp = 60.dp
    actual val contentVerticalPadding: Dp = 8.dp
}
