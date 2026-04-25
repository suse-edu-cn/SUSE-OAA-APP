package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PullUpFeatureDrawer(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    bottomBarHeight: Dp = 0.dp,
    modifier: Modifier = Modifier,
    drawerBlurRadius: Dp = 10.dp,
    drawerTintAlpha: Float = 0.5f,
    drawerNoiseFactor: Float = 0.02f,
    // 返回手势进度 (null=无手势, 0~1=手势进行中)
    backGestureProgress: Float? = null,
    // 手势取消计数，每次取消自增，触发弹回动画
    backGestureCancelCount: Int = 0,
    baseContent: @Composable () -> Unit,
    drawerContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var maxPx by rememberSaveable { mutableFloatStateOf(0f) }
    val peekHeight = 60.dp + bottomBarHeight
    val peekPx = with(density) { peekHeight.toPx() }

    val hazeState = remember { HazeState() }

    val offsetYAnim = remember { Animatable(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    val expandedOffset = maxPx * 0.1f
    val collapsedOffset = maxPx - peekPx
    val dragRange = (collapsedOffset - expandedOffset).coerceAtLeast(1f)

    // 布局尺寸变化（旋转屏幕、bottomBar 高度测量）→ 始终 snapTo，不播放动画
    LaunchedEffect(maxPx, peekPx) {
        if (maxPx > 0f) {
            val target = if (isExpanded) expandedOffset else collapsedOffset
            if (!isInitialized) {
                offsetYAnim.snapTo(target)
                isInitialized = true
            } else if (!offsetYAnim.isRunning) {
                offsetYAnim.snapTo(target)
            }
        }
    }

    // isExpanded 状态变化（返回键折叠）→ 平滑动画
    LaunchedEffect(isExpanded) {
        if (maxPx > 0f && isInitialized && !offsetYAnim.isRunning) {
            val target = if (isExpanded) expandedOffset else collapsedOffset
            if (!isExpanded) {
                offsetYAnim.animateTo(
                    target,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
                )
            } else {
                offsetYAnim.snapTo(target)
            }
        }
    }

    // 预测性返回手势进度 → 实时驱动抽屉位置
    LaunchedEffect(backGestureProgress) {
        if (backGestureProgress != null && maxPx > 0f && isInitialized) {
            val target = (expandedOffset + backGestureProgress * dragRange)
                .coerceIn(expandedOffset, collapsedOffset)
            offsetYAnim.snapTo(target)
        }
    }

    // 手势取消 → 从当前位置弹回展开状态
    LaunchedEffect(backGestureCancelCount) {
        if (backGestureCancelCount > 0 && maxPx > 0f && isInitialized) {
            offsetYAnim.animateTo(
                expandedOffset,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val current = offsetYAnim.value
            val newOffset = current + delta
            val resistantOffset = if (newOffset < expandedOffset && delta < 0) {
                current + delta * 0.3f
            } else {
                newOffset
            }
            offsetYAnim.snapTo(resistantOffset)
        }
    }

    val settleToTarget = { velocity: Float ->
        coroutineScope.launch {
            val current = offsetYAnim.value
            val target = if (velocity < -500f) {
                expandedOffset
            } else if (velocity > 500f) {
                collapsedOffset
            } else {
                if (current < collapsedOffset - dragRange * 0.5f) expandedOffset else collapsedOffset
            }

            val nextExpanded = target == expandedOffset
            if (nextExpanded != isExpanded) {
                onExpandedChange(nextExpanded)
            }

            offsetYAnim.animateTo(
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    val inferredOffset = when {
        maxPx > 0f -> if (isExpanded) expandedOffset else collapsedOffset
        isExpanded -> 0f
        else -> 10000f  // 布局测量前置于屏幕外，避免初始闪现展开状态
    }
    val safeOffset = when {
        !isInitialized -> inferredOffset
        else -> offsetYAnim.value
    }

    val progress = if (maxPx <= 0f || dragRange <= 0f) 0f else {
        (1f - ((safeOffset - expandedOffset) / dragRange)).coerceIn(0f, 1f)
    }

    val scaleFactor = 1f - (0.08f * progress)
    val yTranslation = -20f * progress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { size ->
                if (size.height > 0) {
                    maxPx = size.height.toFloat()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = peekHeight)
                .hazeSource(state = hazeState)
                .graphicsLayer {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    translationY = yTranslation
                    shadowElevation = 0f
                    shape = RoundedCornerShape(36.dp * progress)
                    clip = true
                }
        ) {
            baseContent()
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (0.3f * progress).coerceIn(0f, 1f)))
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, safeOffset.roundToInt()) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> settleToTarget(velocity) }
                )
                .clip(shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        tint = HazeTint(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                .copy(alpha = drawerTintAlpha)
                        ),
                        blurRadius = drawerBlurRadius,
                        noiseFactor = drawerNoiseFactor
                    )
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                drawerContent()
            }
        }
    }
}
