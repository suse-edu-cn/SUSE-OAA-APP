package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口大小类别 - 遵循 Material Design 3 响应式布局规范
 */
enum class WindowSizeClass {
    /** 紧凑型 - 手机竖屏 (< 600dp) */
    COMPACT,

    /** 中等型 - 大手机/小平板 (600dp - 840dp) */
    MEDIUM,

    /** 扩展型 - 平板/桌面 (> 840dp) */
    EXPANDED
}

/**
 * 自适应布局配置
 */
data class AdaptiveLayoutConfig(
    val windowSizeClass: WindowSizeClass,
    val screenWidth: Dp,
    val screenHeight: Dp,
    /** 是否为平板设备 */
    val isTablet: Boolean,
    /** 是否为横屏模式 */
    val isLandscape: Boolean,
    /** 是否使用侧边导航栏（平板横屏时使用） */
    val useSideNavigation: Boolean,
    /** 推荐的网格列数 */
    val gridColumns: Int,
    /** 推荐的内容最大宽度 */
    val maxContentWidth: Dp,
    /** 推荐的水平内边距 */
    val horizontalPadding: Dp,
    /** 是否使用双面板布局 */
    val useTwoPaneLayout: Boolean,
    /** 侧边面板宽度（双面板布局时使用）*/
    val sidePanelWidth: Dp,
    /** 侧边导航栏宽度 */
    val navigationRailWidth: Dp
) {
    companion object {
        /**
         * 根据屏幕宽度计算布局配置
         */
        fun calculate(screenWidth: Dp, screenHeight: Dp): AdaptiveLayoutConfig {
            val windowSizeClass = when {
                screenWidth < 600.dp -> WindowSizeClass.COMPACT
                screenWidth < 840.dp -> WindowSizeClass.MEDIUM
                else -> WindowSizeClass.EXPANDED
            }

            val isTablet = screenWidth >= 600.dp || screenHeight >= 600.dp
            val isLandscape = screenWidth > screenHeight

            // 平板横屏时使用侧边导航
            val useSideNavigation = isTablet && isLandscape

            val gridColumns = when (windowSizeClass) {
                WindowSizeClass.COMPACT -> 2
                WindowSizeClass.MEDIUM -> 3
                WindowSizeClass.EXPANDED -> 4
            }

            val maxContentWidth = when (windowSizeClass) {
                WindowSizeClass.COMPACT -> screenWidth
                WindowSizeClass.MEDIUM -> 840.dp
                WindowSizeClass.EXPANDED -> 1200.dp
            }

            val horizontalPadding = when (windowSizeClass) {
                WindowSizeClass.COMPACT -> 16.dp
                WindowSizeClass.MEDIUM -> 24.dp
                WindowSizeClass.EXPANDED -> 32.dp
            }

            val useTwoPaneLayout = windowSizeClass == WindowSizeClass.EXPANDED

            val sidePanelWidth = when (windowSizeClass) {
                WindowSizeClass.COMPACT -> 0.dp
                WindowSizeClass.MEDIUM -> 280.dp
                WindowSizeClass.EXPANDED -> 320.dp
            }

            val navigationRailWidth = 80.dp

            return AdaptiveLayoutConfig(
                windowSizeClass = windowSizeClass,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                isTablet = isTablet,
                isLandscape = isLandscape,
                useSideNavigation = useSideNavigation,
                gridColumns = gridColumns,
                maxContentWidth = maxContentWidth,
                horizontalPadding = horizontalPadding,
                useTwoPaneLayout = useTwoPaneLayout,
                sidePanelWidth = sidePanelWidth,
                navigationRailWidth = navigationRailWidth
            )
        }
    }
}

/**
 * 自适应布局容器 - 自动检测屏幕尺寸并提供布局配置
 *
 * @param modifier 修饰符
 * @param content 内容 lambda，提供 AdaptiveLayoutConfig 配置
 */
@Composable
fun AdaptiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(AdaptiveLayoutConfig) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val config = remember(maxWidth, maxHeight) {
            AdaptiveLayoutConfig.calculate(maxWidth, maxHeight)
        }
        content(config)
    }
}

/**
 * 根据布局配置获取推荐的 GridCells
 */
fun AdaptiveLayoutConfig.toGridCells(): GridCells = GridCells.Fixed(gridColumns)

/**
 * 获取卡片列表的推荐列数
 *
 * @param minItemWidth 每个卡片的最小宽度
 */
fun AdaptiveLayoutConfig.getCardColumns(minItemWidth: Dp = 160.dp): Int {
    val availableWidth = screenWidth - (horizontalPadding * 2)
    val calculatedColumns = (availableWidth / minItemWidth).toInt()
    return calculatedColumns.coerceIn(1, 4)
}

/**
 * 获取详情页面的推荐列数
 */
fun AdaptiveLayoutConfig.getDetailColumns(): Int = when (windowSizeClass) {
    WindowSizeClass.COMPACT -> 1
    WindowSizeClass.MEDIUM -> 2
    WindowSizeClass.EXPANDED -> 2
}

/**
 * 获取列表项的推荐列数
 */
fun AdaptiveLayoutConfig.getListColumns(): Int = when (windowSizeClass) {
    WindowSizeClass.COMPACT -> 1
    WindowSizeClass.MEDIUM -> 2
    WindowSizeClass.EXPANDED -> 3
}
