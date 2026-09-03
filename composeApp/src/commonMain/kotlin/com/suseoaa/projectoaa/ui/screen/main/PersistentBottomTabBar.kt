package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 常驻的手机端底部导航栏。
 *
 * 早先它是 `PhoneLayout`（即 `MainScreen` 的一部分）里的内容，而 `MainScreen` 本身是导航图里
 * 一个普通的 `composable` 目的地，会随每次页面跳转被套上缩放/圆角裁剪转场动画（见
 * [SharedNavHost][com.suseoaa.projectoaa.ui.navigation.SharedNavHost]）。底部栏自带的毛玻璃
 * 模糊（`hazeEffect`）和阴影（`Modifier.shadow`）在 iOS 上本就比 Android 貴，一旦又被这层转场
 * 动画逐帧缩放/裁剪，两者叠加会在 iOS 上明显掉帧。
 *
 * 现在把它提到 `AppNavHost` 外层（[App][com.suseoaa.projectoaa.App]），作为常驻 UI 渲染，
 * 就不会再被任何页面级转场动画影响；只在当前处于 `Screen.Main` 目的地时可见。
 *
 * 分页状态 [pagerState] 由调用方持有并同时传给 [MainScreen] 内部真正渲染 4 个 Tab 内容的
 * `HorizontalPager`，两者共享同一个 `PagerState`，因此拖拽指示器、点击切换、手指滑动三者
 * 的联动逻辑跟原来完全一致，只是不再和底部栏物理上耦合在同一个可动画的目的地内容里。
 */
@Composable
fun PersistentBottomTabBar(
    visible: Boolean,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    pagerState: PagerState,
    hazeState: HazeState,
    isLiquidGlassTabbarEnabled: Boolean,
    liquidGlassTabbarStyle: Int,
    onBottomBarHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var isIndicatorDragging by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var dragIndicatorProgress by remember { mutableStateOf<Float?>(null) }
    val tabIndicatorProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (MainTab.entries.size - 1).toFloat())
        }
    }
    var isFirstComposition by remember { mutableStateOf(true) }

    // 手势滑动完成后，同步 settledPage 到 selectedTab。
    // 程序化跨页动画期间通过检测 scrollJob?.isActive 来避免目标页被中途页覆盖。
    // 使用 isFirstComposition 忽略初始化的首次执行，避免后台恢复时旧的 settledPage 覆盖正确的 selectedTab。
    LaunchedEffect(pagerState.settledPage, isIndicatorDragging) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (!isIndicatorDragging && scrollJob?.isActive != true && pagerState.settledPage != selectedTab) {
            onTabChange(pagerState.settledPage)
        }
    }

    // 若外部或点击产生的 selectedTab 变化，控制 pager 滚动
    LaunchedEffect(selectedTab) {
        val isNotAtTarget = pagerState.currentPage != selectedTab ||
            abs(pagerState.currentPageOffsetFraction) > 0.001f
        if (!isIndicatorDragging && isNotAtTarget) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                val maxIndex = MainTab.entries.size - 1
                val startProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, maxIndex.toFloat())
                val targetProgress = selectedTab.toFloat().coerceIn(0f, maxIndex.toFloat())
                val distance = abs(targetProgress - startProgress)
                val durationMillis = (220 + (distance * 140).toInt()).coerceAtMost(680)

                val startNanos = withFrameNanos { it }
                var rawFraction = 0f
                while (rawFraction < 1f) {
                    val nowNanos = withFrameNanos { it }
                    val elapsedMs = ((nowNanos - startNanos) / 1_000_000f)
                    rawFraction = if (durationMillis <= 0) 1f else (elapsedMs / durationMillis).coerceIn(0f, 1f)
                    val easedFraction = FastOutSlowInEasing.transform(rawFraction)

                    val progress = startProgress + (targetProgress - startProgress) * easedFraction
                    val page = progress.roundToInt().coerceIn(0, maxIndex)
                    val offsetFraction = (progress - page).coerceIn(-0.5f, 0.5f)
                    pagerState.scrollToPage(page = page, pageOffsetFraction = offsetFraction)
                }

                pagerState.scrollToPage(page = selectedTab, pageOffsetFraction = 0f)

                // 这样能够防止因为布局刷新延迟导致的 selectedTab 回退问题
                snapshotFlow { pagerState.settledPage }.first { it == selectedTab }
            }
        }
    }

    // 拖拽释放后，保持选中栏停在目标位，直到 Pager 真正滚动到位再释放覆盖状态
    LaunchedEffect(
        pagerState.isScrollInProgress,
        tabIndicatorProgress,
        dragIndicatorProgress,
        isIndicatorDragging
    ) {
        val pinnedProgress = dragIndicatorProgress ?: return@LaunchedEffect
        if (!isIndicatorDragging && !pagerState.isScrollInProgress &&
            abs(tabIndicatorProgress - pinnedProgress) < 0.001f
        ) {
            dragIndicatorProgress = null
        }
    }

    // 拖拽与吸附动画期间，按选中栏进度实时驱动页面位置
    LaunchedEffect(dragIndicatorProgress, isIndicatorDragging) {
        if (!isIndicatorDragging) return@LaunchedEffect
        val progress = dragIndicatorProgress ?: return@LaunchedEffect
        val maxIndex = MainTab.entries.size - 1
        val safeProgress = progress.coerceIn(0f, maxIndex.toFloat())
        val page = safeProgress.roundToInt().coerceIn(0, maxIndex)
        val offsetFraction = (safeProgress - page).coerceIn(-0.5f, 0.5f)
        pagerState.scrollToPage(page = page, pageOffsetFraction = offsetFraction)
    }

    val displayedIndicatorProgress = dragIndicatorProgress ?: tabIndicatorProgress

    // 原生液态玻璃样式2下，底部栏由 Android 端 LiquidGlassBackdropWrapper 原生渲染，
    // 这里不再重复绘制 Compose 版本；但上面的分页同步逻辑必须保留（不受此条件影响），
    // 否则原生栏切换 Tab 时 Pager 不会跟着联动。
    if (isLiquidGlassTabbarEnabled && liquidGlassTabbarStyle == 2) return

    OaaBottomBar(
        selectedIndex = selectedTab,
        indicatorProgress = displayedIndicatorProgress,
        onIndicatorDrag = { deltaProgress ->
            val maxProgress = (MainTab.entries.size - 1).toFloat()
            val baseProgress = dragIndicatorProgress ?: tabIndicatorProgress
            val newProgress = (baseProgress + deltaProgress).coerceIn(0f, maxProgress)
            isIndicatorDragging = true
            dragIndicatorProgress = newProgress
        },
        onIndicatorDragEnd = {
            val maxIndex = MainTab.entries.size - 1
            val startProgress = (dragIndicatorProgress ?: tabIndicatorProgress)
                .coerceIn(0f, maxIndex.toFloat())
            val targetIndex = startProgress.roundToInt().coerceIn(0, maxIndex)
            val targetProgress = targetIndex.toFloat()

            scope.launch {
                val snapAnim = Animatable(startProgress)
                snapAnim.animateTo(
                    targetValue = targetProgress,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) {
                    val animatedProgress = value.coerceIn(0f, maxIndex.toFloat())
                    dragIndicatorProgress = animatedProgress
                }

                dragIndicatorProgress = targetProgress
                isIndicatorDragging = false

                if (targetIndex != selectedTab) {
                    onTabChange(targetIndex)
                } else if (!pagerState.isScrollInProgress &&
                    abs(tabIndicatorProgress - targetProgress) < 0.001f
                ) {
                    // 已在目标页且对齐完成，立即交回给 Pager 进度驱动
                    dragIndicatorProgress = null
                }
            }
        },
        onNavigate = { index ->
            isIndicatorDragging = false
            // 点击导航时立即释放拖拽残留覆盖态，避免指示器停留在旧位置。
            dragIndicatorProgress = null
            if (selectedTab != index) {
                onTabChange(index)
            }
        },
        hazeState = hazeState,
        isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
        liquidGlassTabbarStyle = liquidGlassTabbarStyle,
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onBottomBarHeightChanged(coordinates.size.height)
            }
    )
}
