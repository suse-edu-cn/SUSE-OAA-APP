package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS 端收紧：Home Indicator 安全区（约 34pt）已经提供了足够的底部留白，
 * 这里只再补一点点间距。
 *
 * barHeight 与 contentVerticalPadding 是配套调整的：单个 Tab 项内容固定需要 44dp
 * （Icon 24dp + Spacer 4dp + Text 16dp），56dp 的栏高减去上下各 6dp 的内边距正好等于
 * 44dp——这与 Android 原始设计（60dp 栏高减去上下各 8dp 内边距同样精确等于 44dp）是同一种
 * “零冗余精确匹配”的比例，不是凭感觉选的数字。不要单独调 barHeight 而不看
 * contentVerticalPadding，否则内容会被压缩（教训见 BottomBarMetrics.kt 的注释）。
 */
actual object BottomBarMetrics {
    actual val outerTopPadding: Dp = 8.dp
    actual val outerBottomPadding: Dp = 6.dp
    actual val barHeight: Dp = 56.dp
    actual val contentVerticalPadding: Dp = 6.dp
}
