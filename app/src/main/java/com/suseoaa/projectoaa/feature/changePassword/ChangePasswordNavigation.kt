package com.suseoaa.projectoaa.feature.changePassword

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val CHANGE_PASSWORD_ROUTE = "change_password_route"

fun NavController.navigateToChangePassword(navOptions: NavOptions? = null) {
    this.navigate(CHANGE_PASSWORD_ROUTE, navOptions)
}

fun NavGraphBuilder.changePasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
    // === 修复：这里移除了 animatedVisibilityScope 参数 ===
) {
    composable(route = CHANGE_PASSWORD_ROUTE) {
        // 在这里，'this' 就是 AnimatedContentScope (实现了 AnimatedVisibilityScope)
        ChangePasswordScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLogin = onNavigateToLogin,
            sharedTransitionScope = sharedTransitionScope,
            // === 修复：使用当前 composable 的作用域 ===
            animatedVisibilityScope = this
        )
    }
}