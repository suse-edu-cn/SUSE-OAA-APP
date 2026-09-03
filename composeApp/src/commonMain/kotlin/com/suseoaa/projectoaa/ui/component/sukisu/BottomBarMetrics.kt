package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部导航栏的尺寸参数。
 *
 * 两端底部系统区域的性质不同，不能共用同一套间距：
 * - Android 的导航栏内边距是功能区，栏体之外还要自己留出视觉间距
 * - iOS 的 Home Indicator 安全区本身就是一段留白，`navigationBarsPadding()` 之后
 *   再叠加同样大小的下边距，整条栏会明显偏高
 *
 * 因此这里按平台给出不同取值，栏体本身的高度也随之微调。
 */
expect object BottomBarMetrics {
    /** 栏体上方的外边距 */
    val outerTopPadding: Dp

    /** 栏体下方、系统安全区之上的外边距 */
    val outerBottomPadding: Dp

    /** 栏体内容区的高度 */
    val barHeight: Dp

    /**
     * 每个 Tab 项内部（图标+间距+文字）上下各留的内边距。
     *
     * 单个 Tab 项的内容固定需要 Icon(24dp) + Spacer(4dp) + Text(labelSmall 行高 16dp) = 44dp，
     * 这个 padding 乘 2 加上 44dp 就是 [barHeight] 的下限——调 [barHeight] 时必须同步检查这个值，
     * 否则内容会被压缩甚至重叠（此前 iOS 端把 barHeight 降到 48dp 但这个 padding 仍是硬编码的
     * 8dp，导致可用空间 32dp < 44dp 内容需求，就是这个问题）。
     */
    val contentVerticalPadding: Dp
}

/** 静置状态下选中气泡的高度，比栏体略矮，上下各留出等量间距 */
val BottomBarMetrics.restingBubbleHeight: Dp get() = barHeight - 8.dp

/** 按压/切换时气泡放大的高度，会有意溢出栏体，形成液态玻璃的挤出效果 */
val BottomBarMetrics.expandedBubbleHeight: Dp get() = barHeight + 24.dp
