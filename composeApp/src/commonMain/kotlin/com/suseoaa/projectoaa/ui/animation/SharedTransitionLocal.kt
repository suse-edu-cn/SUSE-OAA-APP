package com.suseoaa.projectoaa.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * 全局共享的过渡动画作用域，用于优雅的共享元素和铺满屏幕动画
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

// 使用物理弹簧模型（Spring）替换基于时间的缓动曲线，以此来获取极具质感的"跟手性"
// 当手指划动返回或者打断动画时，Spring 会自动结合手指的初始速度计算轨迹，杜绝“慢半拍”和动画拖沓生硬的问题。
val elegantSpringTransform = spring<androidx.compose.ui.geometry.Rect>(
    dampingRatio = 0.85f, // 弱阻尼，带来轻微的、柔和的弹性反馈，使得释放后不那么生硬
    stiffness = 300f     // 整体动画速度相对轻快，顺滑回到原位
)

/**
 * 优雅的共享元素边界动画扩展。
 * 将此 Modifier 添加到需要实现"从方块展开到全屏"的组件上。
 * 
 * @param key 共享元素的唯一标识符。在列表页方块和详情页的外层容器保持一致。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedBoundsTransition(key: String): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> elegantSpringTransform }
            )
        }
    } else {
        this
    }
}

/**
 * 优雅的共享元素内容动画扩展。（如果在展开时需要内部也保持同步的过度）
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElementTransition(key: String): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> elegantSpringTransform }
            )
        }
    } else {
        this
    }
}
