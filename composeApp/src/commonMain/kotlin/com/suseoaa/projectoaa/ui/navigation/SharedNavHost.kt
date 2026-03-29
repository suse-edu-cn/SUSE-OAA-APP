package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
            CompositionLocalProvider(
                LocalSharedTransitionScope provides sharedTransitionScope,
                LocalAnimatedVisibilityScope provides this@composable
            ) {
                content(backStackEntry)
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
