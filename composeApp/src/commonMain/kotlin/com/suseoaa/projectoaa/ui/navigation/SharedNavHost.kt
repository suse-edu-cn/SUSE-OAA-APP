package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.suseoaa.projectoaa.ui.animation.LocalAnimatedVisibilityScope
import com.suseoaa.projectoaa.ui.animation.LocalDisableSharedTransition
import com.suseoaa.projectoaa.ui.animation.LocalPredictiveBackCommitting
import com.suseoaa.projectoaa.ui.animation.LocalSharedTransitionScope
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.enterScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.exitScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.popEnterScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.popExitScale
import com.suseoaa.projectoaa.util.PlatformBackSwipeEdge
import com.suseoaa.projectoaa.util.PlatformPredictiveBackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

data class GestureBackTransformState(
    val enabled: Boolean = false,
    val progress: Float = 0f,
    val swipeEdge: PlatformBackSwipeEdge = PlatformBackSwipeEdge.Left,
    val verticalPosition: Float = 0.5f
)

private val LocalGestureBackTransformState = compositionLocalOf { GestureBackTransformState() }
private val LocalGestureBackTargetEntryId = compositionLocalOf<String?> { null }

private const val PREDICTIVE_VISUAL_ACTIVATION_PROGRESS = 0.10f
private const val PREDICTIVE_QUICK_BACK_MAX_PROGRESS = 0.14f
private const val PREDICTIVE_FAST_SWIPE_DELTA_THRESHOLD = 0.09f
private const val PREDICTIVE_FAST_SWIPE_MAX_PROGRESS = 0.45f
private const val ENTRY_INTERRUPT_WINDOW_MILLIS = 120L

private typealias DestinationContent = @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.PredictiveBackBackgroundLayer(
    previousBackStackEntry: NavBackStackEntry?,
    destinationContentMap: Map<String, DestinationContent>,
    visible: Boolean,
    progress: Float
) {
    if (!visible || previousBackStackEntry == null) return

    val route = previousBackStackEntry.destination.route ?: return
    val destinationContent = destinationContentMap[route] ?: return

    AnimatedVisibility(
        visible = true,
        enter = EnterTransition.None,
        exit = ExitTransition.None
    ) {
        val normalizedProgress = progress.coerceIn(0f, 1f)
        val curvedProgress = normalizedProgress
        // 背景层避免缩到 1 以下，否则会在边缘露出主题纯色底。
        val backgroundScale = 1.04f - (0.04f * curvedProgress)

        CompositionLocalProvider(
            LocalSharedTransitionScope provides this@PredictiveBackBackgroundLayer,
            LocalAnimatedVisibilityScope provides this,
            // 背景预览层仅用于露底展示，不参与共享元素配对。
            LocalDisableSharedTransition provides true
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backgroundScale
                        scaleY = backgroundScale
                    }
            ) {
                destinationContent(previousBackStackEntry)
            }
        }
    }
}

private fun Modifier.gestureBackTransform(
    progress: Float,
    swipeEdge: PlatformBackSwipeEdge,
    verticalPosition: Float,
    cornerRadius: Dp,
    shadowElevation: Dp
): Modifier {
    return graphicsLayer {
        val normalizedProgress = progress.coerceIn(0f, 1f)
        // 采用线性映射，保证不同 ROM 下手势速率更可预期。
        val curvedProgress = normalizedProgress
        // 缩放下限锁定到 80%。
        val scale = (1f - (0.20f * curvedProgress)).coerceAtLeast(0.80f)

        scaleX = scale
        scaleY = scale

        // 满进度时保证对侧边缘保留约 5% 间距。
        val horizontalOffset = size.width * 0.05f * curvedProgress

        // 按手指纵向位置跟手偏移：顶部为负、底部为正，中线附近接近 0。
        val verticalOffset =
            ((verticalPosition.coerceIn(0f, 1f) - 0.5f) * size.height * 0.20f) * curvedProgress

        translationX = when (swipeEdge) {
            PlatformBackSwipeEdge.Right -> -horizontalOffset
            PlatformBackSwipeEdge.Left -> horizontalOffset
        }
        translationY = verticalOffset

        // 圆角与阴影和缩放偏移保持在同一变换层，确保只作用于手势拖动的上层页面。
        shape = RoundedCornerShape(cornerRadius)
        clip = true
        this.shadowElevation = shadowElevation.toPx()
    }
}

/**
 * 支持共享元素过渡的 NavGraphBuilder 包装器
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedTransitionNavGraphBuilder(
    val sharedTransitionScope: SharedTransitionScope,
    val builder: NavGraphBuilder,
    private val destinationContentMap: MutableMap<String, DestinationContent>
) {
    fun composable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        deepLinks: List<NavDeepLink> = emptyList(),
        content: DestinationContent
    ) {
        destinationContentMap[route] = content

        builder.composable(route, arguments, deepLinks) { backStackEntry ->
            val gestureBackTransformState = LocalGestureBackTransformState.current
            val activeBackStackEntryId = LocalGestureBackTargetEntryId.current

            // 从当前作用域中拿到 NavHost 共享过来的退出/进入转场状态
            val cornerRadius by transition.animateDp(label = "cornerRadius") { state ->
                // 当处于退出后的后置阶段（手势拖动或完成离开），赋予 28.dp 圆角，否则 0.dp
                if (state == EnterExitState.PostExit) 28.dp else 0.dp
            }

            CompositionLocalProvider(
                LocalSharedTransitionScope provides sharedTransitionScope,
                LocalAnimatedVisibilityScope provides this@composable
            ) {
                // 判断当前页面是否正处于离开中或处于后台隐藏堆栈中
                val isNotVisible = transition.targetState != EnterExitState.Visible
                val isGestureTarget =
                    gestureBackTransformState.enabled &&
                        backStackEntry.id == activeBackStackEntryId
                val gestureProgress = gestureBackTransformState.progress.coerceIn(0f, 1f)
                val effectiveCornerRadius = if (isGestureTarget) {
                    val gestureCornerRadius = 44.dp * gestureProgress
                    if (gestureCornerRadius > cornerRadius) gestureCornerRadius else cornerRadius
                } else {
                    cornerRadius
                }
                val gestureShadowElevation = 10.dp * gestureProgress

                // 将内容用 Box 包裹，配合 Modifier.clip 裁剪，渲染出动态跟手圆角
                // 使用 pointerInput 拦截隐藏或离开状态页面的点击事件
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isGestureTarget) {
                                Modifier.gestureBackTransform(
                                    progress = gestureBackTransformState.progress,
                                    swipeEdge = gestureBackTransformState.swipeEdge,
                                    verticalPosition = gestureBackTransformState.verticalPosition,
                                    cornerRadius = effectiveCornerRadius,
                                    shadowElevation = gestureShadowElevation
                                )
                            } else {
                                Modifier.clip(RoundedCornerShape(effectiveCornerRadius))
                            }
                        )
                        .then(
                            if (isNotVisible) {
                                Modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    content(backStackEntry)
                }
            }
        }
    }
}

/**
 * 带有优雅缩放退场及共享元素支持的加强版 NavHost
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: SharedTransitionNavGraphBuilder.() -> Unit
) {
    var isPredictiveBackActive by remember { mutableStateOf(false) }
    var isPredictiveBackCancelling by remember { mutableStateOf(false) }
    var isPredictiveBackCommitting by remember { mutableStateOf(false) }
    var hasPredictiveBackProgress by remember { mutableStateOf(false) }
    var suppressNextPopTransition by remember { mutableStateOf(false) }
    var pendingPopCleanup by remember { mutableStateOf(false) }
    var gestureBackEntryId by remember { mutableStateOf<String?>(null) }
    var hasShownPredictiveVisual by remember { mutableStateOf(false) }
    var predictiveBackgroundEntry by remember { mutableStateOf<NavBackStackEntry?>(null) }
    var gestureMaxProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackEdge by remember { mutableStateOf(PlatformBackSwipeEdge.Left) }
    var predictiveBackVerticalPosition by remember { mutableFloatStateOf(0.5f) }
    var lastPredictiveSampleProgress by remember { mutableFloatStateOf(0f) }
    var maxPredictiveProgressDelta by remember { mutableFloatStateOf(0f) }
    var isInEntryAnimationWindow by remember { mutableStateOf(false) }
    var skipEntryInterruptWindowOnce by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val previousBackStackEntry = navController.previousBackStackEntry
    val destinationContentMap = remember { mutableMapOf<String, DestinationContent>() }
    val scope = rememberCoroutineScope()
    var settleAnimationJob by remember { mutableStateOf<Job?>(null) }

    val isPredictiveVisualActive =
        hasShownPredictiveVisual && (
            isPredictiveBackActive ||
                isPredictiveBackCancelling ||
                isPredictiveBackCommitting ||
                pendingPopCleanup
            )

    // 仅在拖动与取消回弹阶段禁用共享元素；commit 阶段需恢复以承接当前位置返回。
    val shouldDisableSharedTransition = isPredictiveBackActive || isPredictiveBackCancelling

    LaunchedEffect(currentBackStackEntry?.id) {
        val entryId = currentBackStackEntry?.id
        if (entryId == null) {
            isInEntryAnimationWindow = false
            return@LaunchedEffect
        }

        if (skipEntryInterruptWindowOnce) {
            // pop 退场后的回到上一页不再延迟判定，避免“动画结束仍需等待”。
            skipEntryInterruptWindowOnce = false
            isInEntryAnimationWindow = false
            return@LaunchedEffect
        }

        isInEntryAnimationWindow = true
        delay(ENTRY_INTERRUPT_WINDOW_MILLIS)
        if (currentBackStackEntry?.id == entryId) {
            isInEntryAnimationWindow = false
        }
    }

    LaunchedEffect(currentBackStackEntry?.id) {
        // commit 后由 back stack 变化触发统一清理，避免松手时先回全屏。
        if (pendingPopCleanup) {
            pendingPopCleanup = false

            if (isPredictiveBackCommitting) {
                // 不再阻塞等待余震收尾，优先释放交互，避免视觉到位后仍无法点击。
                settleAnimationJob?.cancel()
                settleAnimationJob = null
            }

            // 若延迟期间用户重新开始手势，则放弃本次清理，避免覆盖新状态。
            if (isPredictiveBackActive || isPredictiveBackCancelling) {
                return@LaunchedEffect
            }

            isPredictiveBackActive = false
            isPredictiveBackCancelling = false
            isPredictiveBackCommitting = false
            hasPredictiveBackProgress = false
            suppressNextPopTransition = false
            gestureBackEntryId = null
            hasShownPredictiveVisual = false
            predictiveBackgroundEntry = null
            gestureMaxProgress = 0f
            predictiveBackProgress = 0f
            predictiveBackVerticalPosition = 0.5f
            lastPredictiveSampleProgress = 0f
            maxPredictiveProgressDelta = 0f
            settleAnimationJob = null
            return@LaunchedEffect
        }

        if (!isPredictiveBackActive && !isPredictiveBackCancelling && !isPredictiveBackCommitting) {
            val hasResidualState =
                suppressNextPopTransition ||
                    hasPredictiveBackProgress ||
                    gestureBackEntryId != null ||
                    hasShownPredictiveVisual ||
                    predictiveBackgroundEntry != null ||
                    gestureMaxProgress > 0f ||
                    predictiveBackProgress != 0f ||
                    predictiveBackVerticalPosition != 0.5f ||
                    lastPredictiveSampleProgress != 0f ||
                    maxPredictiveProgressDelta > 0f

            if (hasResidualState) {
                hasPredictiveBackProgress = false
                suppressNextPopTransition = false
                gestureBackEntryId = null
                hasShownPredictiveVisual = false
                predictiveBackgroundEntry = null
                gestureMaxProgress = 0f
                predictiveBackProgress = 0f
                predictiveBackVerticalPosition = 0.5f
                lastPredictiveSampleProgress = 0f
                maxPredictiveProgressDelta = 0f
            }
        }
    }

    val gestureBackTransformState = GestureBackTransformState(
        enabled = isPredictiveVisualActive,
        progress = predictiveBackProgress,
        swipeEdge = predictiveBackEdge,
        verticalPosition = predictiveBackVerticalPosition
    )
    val activeGestureTargetEntryId = gestureBackEntryId ?: currentBackStackEntry?.id
    val backgroundEntryToRender = predictiveBackgroundEntry ?: previousBackStackEntry
    val showPredictiveBackground =
        hasShownPredictiveVisual &&
            (
                isPredictiveBackActive ||
                    isPredictiveBackCancelling ||
                    isPredictiveBackCommitting ||
                    pendingPopCleanup
                ) &&
            backgroundEntryToRender != null

    SharedTransitionLayout {
        Box(modifier = Modifier.fillMaxSize()) {
            this@SharedTransitionLayout.PredictiveBackBackgroundLayer(
                previousBackStackEntry = backgroundEntryToRender,
                destinationContentMap = destinationContentMap,
                visible = showPredictiveBackground,
                progress = predictiveBackProgress
            )

            CompositionLocalProvider(
                LocalGestureBackTransformState provides gestureBackTransformState,
                LocalGestureBackTargetEntryId provides activeGestureTargetEntryId,
                LocalPredictiveBackCommitting provides isPredictiveBackCommitting,
                LocalDisableSharedTransition provides shouldDisableSharedTransition
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = modifier,
                    // 优雅的缩放动画架构作为底部界面的兜底动画
                    enterTransition = { enterScale() },
                    exitTransition = { exitScale() },
                    popEnterTransition = {
                        if (suppressNextPopTransition) {
                            EnterTransition.None
                        } else {
                            popEnterScale()
                        }
                    },
                    popExitTransition = {
                        if (suppressNextPopTransition) {
                            ExitTransition.None
                        } else {
                            popExitScale()
                        }
                    }
                ) {
                    val sharedBuilder = SharedTransitionNavGraphBuilder(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        builder = this,
                        destinationContentMap = destinationContentMap
                    )
                    sharedBuilder.builder()
                }
            }
        }

        // 放在 NavHost 之后声明，确保手势回调优先级高于 NavHost 内部默认回调。
        PlatformPredictiveBackHandler(
            enabled = navController.previousBackStackEntry != null,
            onProgress = { event ->
                settleAnimationJob?.cancel()
                settleAnimationJob = null
                isPredictiveBackCancelling = false
                isPredictiveBackCommitting = false
                pendingPopCleanup = false

                val widthProgress = event.distanceProgress
                val rawProgress = if (widthProgress.isFinite()) {
                    widthProgress.coerceIn(0f, 1f)
                } else {
                    event.progress.coerceIn(0f, 1f)
                }

                if (gestureBackEntryId == null) {
                    gestureBackEntryId = navController.currentBackStackEntry?.id
                }

                if (!hasPredictiveBackProgress) {
                    lastPredictiveSampleProgress = rawProgress
                    maxPredictiveProgressDelta = 0f
                } else {
                    val delta = (rawProgress - lastPredictiveSampleProgress).coerceAtLeast(0f)
                    maxPredictiveProgressDelta = max(maxPredictiveProgressDelta, delta)
                    lastPredictiveSampleProgress = rawProgress
                }

                gestureMaxProgress = max(gestureMaxProgress, rawProgress)
                hasPredictiveBackProgress = true
                predictiveBackProgress = rawProgress
                predictiveBackEdge = event.swipeEdge
                predictiveBackVerticalPosition = event.verticalPosition.coerceIn(0f, 1f)

                // 仅在手势位移达到阈值后才进入预测返回可视态，避免“快速返回”误触预测动画。
                if (
                    !hasShownPredictiveVisual &&
                    rawProgress >= PREDICTIVE_VISUAL_ACTIVATION_PROGRESS
                ) {
                    hasShownPredictiveVisual = true
                    predictiveBackgroundEntry = navController.previousBackStackEntry
                }

                isPredictiveBackActive = hasShownPredictiveVisual
            },
            onCancel = {
                settleAnimationJob?.cancel()
                settleAnimationJob = null

                if (hasShownPredictiveVisual) {
                    isPredictiveBackActive = false
                    isPredictiveBackCancelling = true
                    isPredictiveBackCommitting = false
                    suppressNextPopTransition = false

                    val startProgress = predictiveBackProgress.coerceIn(0f, 1f)
                    settleAnimationJob = scope.launch {
                        val animatable = Animatable(startProgress)
                        try {
                            animatable.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                            ) {
                                predictiveBackProgress = value
                            }
                        } catch (_: CancellationException) {
                            return@launch
                        }

                        isPredictiveBackCancelling = false
                        isPredictiveBackCommitting = false
                        hasPredictiveBackProgress = false
                        gestureBackEntryId = null
                        hasShownPredictiveVisual = false
                        predictiveBackgroundEntry = null
                        gestureMaxProgress = 0f
                        predictiveBackProgress = 0f
                        predictiveBackVerticalPosition = 0.5f
                        lastPredictiveSampleProgress = 0f
                        maxPredictiveProgressDelta = 0f
                    }
                } else {
                    isPredictiveBackActive = false
                    isPredictiveBackCancelling = false
                    isPredictiveBackCommitting = false
                    suppressNextPopTransition = false
                    hasPredictiveBackProgress = false
                    gestureBackEntryId = null
                    hasShownPredictiveVisual = false
                    predictiveBackgroundEntry = null
                    gestureMaxProgress = 0f
                    predictiveBackProgress = 0f
                    predictiveBackVerticalPosition = 0.5f
                    lastPredictiveSampleProgress = 0f
                    maxPredictiveProgressDelta = 0f
                }
            },
            onBack = {
                settleAnimationJob?.cancel()
                settleAnimationJob = null

                val isFastSwipeGesture =
                    maxPredictiveProgressDelta >= PREDICTIVE_FAST_SWIPE_DELTA_THRESHOLD &&
                        gestureMaxProgress <= PREDICTIVE_FAST_SWIPE_MAX_PROGRESS

                // 页面刚进场时允许返回手势直接打断，统一走共享快速退场。
                val shouldInterruptEnterAnimation =
                    isInEntryAnimationWindow && navController.previousBackStackEntry != null

                val shouldUseSharedForQuickBack =
                    shouldInterruptEnterAnimation ||
                        isFastSwipeGesture || (
                        predictiveBackProgress < PREDICTIVE_QUICK_BACK_MAX_PROGRESS &&
                        gestureMaxProgress < PREDICTIVE_QUICK_BACK_MAX_PROGRESS
                        )

                val shouldUsePredictiveVisual =
                    hasShownPredictiveVisual && !shouldUseSharedForQuickBack

                if (shouldUseSharedForQuickBack) {
                    isPredictiveBackActive = false
                    isPredictiveBackCancelling = false
                    isPredictiveBackCommitting = false
                    suppressNextPopTransition = shouldInterruptEnterAnimation
                    pendingPopCleanup = shouldInterruptEnterAnimation
                    if (!hasPredictiveBackProgress) {
                        predictiveBackProgress = 0f
                    }
                    hasPredictiveBackProgress = false
                    gestureBackEntryId = null
                    hasShownPredictiveVisual = false
                    predictiveBackgroundEntry = null
                    gestureMaxProgress = 0f
                    predictiveBackVerticalPosition = 0.5f
                    lastPredictiveSampleProgress = 0f
                    maxPredictiveProgressDelta = 0f
                    skipEntryInterruptWindowOnce = true
                    navController.popBackStack()
                } else if (shouldUsePredictiveVisual) {
                    isPredictiveBackActive = false
                    isPredictiveBackCancelling = false
                    isPredictiveBackCommitting = false
                    suppressNextPopTransition = false
                    pendingPopCleanup = true
                    predictiveBackProgress = predictiveBackProgress.coerceIn(0f, 1f)
                    skipEntryInterruptWindowOnce = true
                    navController.popBackStack()
                } else {
                    isPredictiveBackActive = false
                    isPredictiveBackCancelling = false
                    isPredictiveBackCommitting = false
                    suppressNextPopTransition = false
                    predictiveBackProgress = 0f
                    hasPredictiveBackProgress = false
                    gestureBackEntryId = null
                    hasShownPredictiveVisual = false
                    predictiveBackgroundEntry = null
                    gestureMaxProgress = 0f
                    predictiveBackVerticalPosition = 0.5f
                    lastPredictiveSampleProgress = 0f
                    maxPredictiveProgressDelta = 0f
                    skipEntryInterruptWindowOnce = true
                    navController.popBackStack()
                }
            }
        )
    }
}
