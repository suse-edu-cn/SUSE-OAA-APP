package com.suseoaa.projectoaa.startHomeNavigation.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

// 定义页面顺序，用于确定动画方向
private val screenOrder = mapOf(
    "home" to 0,
    "search" to 1,
    "settings" to 2,
    "profile" to 3
)

// 根据导航方向确定动画方向
fun getEnterTransition(isForward: Boolean): EnterTransition {
    return if (isForward) {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    } else {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }
}

fun getExitTransition(isForward: Boolean): ExitTransition {
    return if (isForward) {
        slideOutHorizontally(
            targetOffsetX = { -it / 2 },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    } else {
        slideOutHorizontally(
            targetOffsetX = { it / 2 },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
}

// 获取导航方向（基于页面在底部导航栏中的顺序）
fun getNavigationDirection(from: String, to: String): Boolean {
    val fromIndex = screenOrder.getOrDefault(from, 0)
    val toIndex = screenOrder.getOrDefault(to, 0)
    return toIndex > fromIndex
}

// 使用一个简单的状态来追踪上一个访问的页面
object NavigationTracker {
    var lastRoute: String = "home"
        private set

    fun updateRoute(newRoute: String) {
        lastRoute = newRoute
    }
}