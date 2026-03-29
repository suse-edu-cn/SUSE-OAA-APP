package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.ui.animation.LocalAnimatedVisibilityScope
import com.suseoaa.projectoaa.ui.animation.LocalSharedTransitionScope
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.enterScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.exitScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.popEnterScale
import com.suseoaa.projectoaa.ui.animation.ScaleTransition.popExitScale

/**
 * 支持共享元素过渡的 NavGraphBuilder 包装器
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedTransitionNavGraphBuilder(
    val sharedTransitionScope: SharedTransitionScope,
    val builder: NavGraphBuilder
) {
    fun composable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        deepLinks: List<NavDeepLink> = emptyList(),
        content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
    ) {
        builder.composable(route, arguments, deepLinks) { backStackEntry ->
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
                
                // 将内容用 Box 包裹，配合 Modifier.clip 裁剪，渲染出动态跟手圆角
                // 使用 pointerInput 拦截隐藏或离开状态页面的点击事件
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
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
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            // 优雅的缩放动画架构作为底部界面的兜底动画
            enterTransition = { enterScale() },
            exitTransition = { exitScale() },
            popEnterTransition = { popEnterScale() },
            popExitTransition = { popExitScale() }
        ) {
            val sharedBuilder = SharedTransitionNavGraphBuilder(this@SharedTransitionLayout, this)
            sharedBuilder.builder()
        }
    }
}
