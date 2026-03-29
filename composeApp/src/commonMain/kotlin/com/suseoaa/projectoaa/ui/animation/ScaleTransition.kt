package com.suseoaa.projectoaa.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

object ScaleTransition {

    // 统一配置全局过渡动画的 Spring 阻尼参数
    // 带有轻微回弹的物理弹簧参数，提供顺滑推背感
    private fun <T> defaultSpring() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = 300f 
    )

    fun AnimatedContentTransitionScope<NavBackStackEntry>.enterScale(): EnterTransition {
        return scaleIn(
            initialScale = 0.85f,
            animationSpec = defaultSpring()
        ) + fadeIn(
            animationSpec = defaultSpring()
        )
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.exitScale(): ExitTransition {
        return scaleOut(
            targetScale = 0.85f,
            animationSpec = defaultSpring()
        )
    }

    // popEnterScale（底层页面的进场）：
    // 真正的 100% 实体进场，去除了任何 fadeIn 遮挡，不漏白底黑底！
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterScale(): EnterTransition {
        return scaleIn(
            initialScale = 0.85f,
            animationSpec = defaultSpring()
        )
    }

    // popExitScale（顶层页面的退场）：
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitScale(): ExitTransition {
        return scaleOut(
            targetScale = 0.85f,
            animationSpec = defaultSpring()
        ) + slideOutHorizontally(
            // 手势拉满时向右推 15% 屏宽，增强物理推背感
            targetOffsetX = { (it * 0.15f).toInt() }, 
            animationSpec = defaultSpring()
        ) + fadeOut(
            // 让淡出动画略微放缓，使用弹簧驱动确保不会一拉就消失
            animationSpec = defaultSpring()
        )
    }
}
