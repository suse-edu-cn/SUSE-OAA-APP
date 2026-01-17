package com.suseoaa.projectoaa.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.feature.academicPortal.AcademicDestinations
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.GetExamInfoScreen
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoScreen
import com.suseoaa.projectoaa.feature.gpa.GpaScreen
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
    SharedTransitionLayout(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
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
                    modifier = Modifier.graphicsLayer { clip = true },
                    onAcademicEvent = { event ->
                        when (event) {
                            is AcademicPortalEvent.NavigateTo -> {
                                navController.navigate(event.destination.route)
                            }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(LOGIN_ROUTE) {
                            // 关键：清空返回栈，防止按返回键回到已退出的页面
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(AcademicDestinations.Grades.route) {
                GradesScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(AcademicDestinations.Exams.route) {
                GetExamInfoScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(AcademicDestinations.Messages.route) {
                GetAcademicMessageInfoScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(AcademicDestinations.Gpa.route) {
                GpaScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(AcademicDestinations.Test.route) {
                TestScreen()
            }
        }
    }
}