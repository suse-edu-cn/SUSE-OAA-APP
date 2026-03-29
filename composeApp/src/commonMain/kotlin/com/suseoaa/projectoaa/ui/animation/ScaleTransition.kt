package com.suseoaa.projectoaa.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

/**
 * 优雅的缩放动画架构
 * 定义了一组可复用的缩放转场动画，采用物理弹簧曲线（Spring）实现极致的跟手与细腻体验
 */
object ScaleTransition {

    // 采用 Spring（弹簧）动画曲线替换 Tween，能完美融入“预测性返回”和手势滑动：
    // Spring 可以自动基于手指最后的速度（Velocity）计算轨迹，实现“跟手且无缝”的过渡。
    private val spatialSpring = spring<Float>(
        dampingRatio = 0.8f, // 略微带有弹性，显得更轻盈不生硬
        stiffness = 380f     // 适当的坚硬度，避免拖沓（约合中低硬度）
    )

    private val alphaSpring = spring<Float>(
        dampingRatio = 1f, // 透明度不需要弹性（DampingRatioNoBouncy）
        stiffness = 400f   // 稍微快一点呈现
    )

    // 入场动画：从屏幕中心内缩状态稍微放大并淡入
    fun AnimatedContentTransitionScope<NavBackStackEntry>.enterScale(): EnterTransition {
        return scaleIn(
            initialScale = 0.8f, // 放大起点幅度加大，展现“跃出”效果
            animationSpec = spatialSpring
        ) + fadeIn(
            animationSpec = alphaSpring
        )
    }

    // 退场动画（被新页面覆盖时）：向后退缩景深，略微缩小
    fun AnimatedContentTransitionScope<NavBackStackEntry>.exitScale(): ExitTransition {
        return scaleOut(
            targetScale = 0.95f, 
            animationSpec = spatialSpring
        ) + fadeOut(
            animationSpec = alphaSpring
        )
    }

    // 弹出（返回：底部原本被覆盖的页面重新展示）：从略小状态放缩回原大小
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterScale(): EnterTransition {
        return scaleIn(
            initialScale = 0.9f, // 更深远的背景放大，配合75%的前景缩小
            animationSpec = spatialSpring
        ) + fadeIn(
            animationSpec = alphaSpring
        )
    }

    // 弹出（返回：当面页面被关闭或滑开退出）：整体向后剧烈退缩并淡出 （实现了预测性缩放的优雅观感）
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitScale(): ExitTransition {
        return scaleOut(
            targetScale = 0.75f, // 退出时向后景深缩小到75%，配合预测性返回手势
            animationSpec = spatialSpring
        ) + fadeOut(
            targetAlpha = 0.0f,
            animationSpec = spring(
                dampingRatio = 1f,
                stiffness = 200f // 淡出慢一点，避免松手前就突然看不见了
            )
        )
    }
}
