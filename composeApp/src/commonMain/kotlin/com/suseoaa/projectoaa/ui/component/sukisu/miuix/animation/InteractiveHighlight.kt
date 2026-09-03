package com.suseoaa.projectoaa.ui.component.sukisu.miuix.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import com.suseoaa.projectoaa.ui.component.sukisu.miuix.modifier.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 按压时跟随手指位置的液态玻璃高光。
 *
 * 原实现用一段 AGSL RuntimeShader 画 `smoothstep(radius, radius*0.5, dist)` 的径向衰减，
 * 这是 Android 专属 API（`android.graphics.RuntimeShader`），iOS 上没有对应实现，
 * 因此原文件被隔离在 androidMain，iOS 端完全没有这个高光。
 *
 * 但这段衰减本质就是一个"内 50% 半径满强度、外 50% 半径线性淡出到 0"的径向渐变，
 * 用 Compose 跨平台的 [Brush.radialGradient] 就能画出几乎一致的视觉效果，
 * 完全不需要自定义 shader，因此把整个高光效果收敛成纯 Compose 实现，
 * 两端共用同一份代码，不再需要 expect/actual。
 */
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    private val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec =
        spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val offset: Offset get() = positionAnimation.value - startPosition

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                drawRect(
                    Color.White.copy(0.06f * progress),
                    blendMode = BlendMode.Plus
                )
                val center = position(size, positionAnimation.value).let {
                    Offset(
                        it.x.fastCoerceIn(0f, size.width),
                        it.y.fastCoerceIn(0f, size.height)
                    )
                }
                val radius = (size.minDimension * 1.2f).coerceAtLeast(1f)
                val glowColor = Color.White.copy(0.12f * progress)
                drawRect(
                    Brush.radialGradient(
                        // 内半径全强度、外半径线性淡出到 0，近似原 shader 的 smoothstep 衰减
                        0f to glowColor,
                        0.5f to glowColor,
                        1f to glowColor.copy(alpha = 0f),
                        center = center,
                        radius = radius
                    ),
                    blendMode = BlendMode.Plus
                )
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}
