package com.suseoaa.projectoaa.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.feature.academicPortal.AcademicDestinations
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen
import com.suseoaa.projectoaa.feature.home.MAIN_SCREEN_ROUTE
import com.suseoaa.projectoaa.feature.home.MainScreen
import com.suseoaa.projectoaa.feature.login.LOGIN_ROUTE
import com.suseoaa.projectoaa.feature.login.loginScreen
import com.suseoaa.projectoaa.feature.register.navigateToRegister
import com.suseoaa.projectoaa.feature.register.registerScreen
import com.suseoaa.projectoaa.feature.testScreen.TestScreen

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
        // 1. 进场动画：新页面从右边滑入 (Towards Start)
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        // 2. 出场动画：旧页面向左边滑出 (Towards Start)
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        // 3. 返回-进场动画：上一页从左边滑回来 (Towards End)
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        // 4. 返回-出场动画：当前页向右边滑走 (Towards End)
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
//        无动画
//        enterTransition = { EnterTransition.None },
//        exitTransition = { ExitTransition.None },
//        popEnterTransition = { EnterTransition.None },
//        popExitTransition = { ExitTransition.None }

    ) {
        loginScreen(
            onLoginSuccess = {
                navController.navigate(MAIN_SCREEN_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigateToRegister()
            }
        )
        registerScreen(
            onRegisterSuccess = {
                navController.popBackStack()
            }
        )
        composable(MAIN_SCREEN_ROUTE) {
            MainScreen(
                windowSizeClass = windowSizeClass,
                onAcademicEvent = { event ->
                    when (event) {
                        is AcademicPortalEvent.NavigateTo -> {
                            navController.navigate(event.destination.route)
                        }
                    }
                }
            )
        }
        //教务信息-成绩查询
        composable(AcademicDestinations.Grades.route) {
            GradesScreen(onBack = { navController.popBackStack() })
        }
//       测试页面
        composable(AcademicDestinations.Test.route) {
            TestScreen()
        }


    }
}