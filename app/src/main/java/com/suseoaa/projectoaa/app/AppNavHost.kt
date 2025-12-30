package com.suseoaa.projectoaa.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.feature.home.MAIN_SCREEN_ROUTE
import com.suseoaa.projectoaa.feature.home.MainScreen
import com.suseoaa.projectoaa.feature.login.LOGIN_ROUTE
import com.suseoaa.projectoaa.feature.login.loginScreen
import com.suseoaa.projectoaa.feature.register.navigateToRegister
import com.suseoaa.projectoaa.feature.register.registerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    windowSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    startDestination: String = LOGIN_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // 1. 登录页
        loginScreen(
            onLoginSuccess = {
                // 登录成功 -> 跳转到主容器页
                navController.navigate(MAIN_SCREEN_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigateToRegister()
            }
        )

        // 2. 注册页
        registerScreen(
            onRegisterSuccess = {
                navController.popBackStack()
            }
        )

        // 3. 主容器页
        composable(MAIN_SCREEN_ROUTE) {
            MainScreen(windowSizeClass = windowSizeClass)
        }
    }
}