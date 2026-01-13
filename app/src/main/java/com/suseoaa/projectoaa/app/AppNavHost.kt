package com.suseoaa.projectoaa.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.core.util.containerVisualPhysics
import com.suseoaa.projectoaa.core.util.keepAlivePhysics
import com.suseoaa.projectoaa.feature.academicPortal.AcademicDestinations
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.GetExamInfoScreen
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoScreen
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

            // 1. 进场
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = containerVisualPhysics()
                )
            },

            // 2. 普通离场 (正向跳转时也要保活，防止平板主页闪烁)
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = containerVisualPhysics()
                ) + fadeOut(
                    animationSpec = keepAlivePhysics()
                )
            },

            // 3. 返回手势 - 顶层页面
            popExitTransition = {
                // 动作A：缩放 (500ms) - 露出底层页面
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = containerVisualPhysics()
                ) +
                        // 动作B：保活淡出 (750ms) - 防止过早销毁
                        fadeOut(
                            animationSpec = keepAlivePhysics()
                        ) +
                        // 动作C：1像素锚点位移 (750ms) - 防止平板渲染引擎偷懒丢动画
                        slideOutHorizontally(
                            targetOffsetX = { 1 },
                            animationSpec = keepAlivePhysics()
                        )
            },

            // 4. 返回手势 - 底层页面
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.96f, // 微微缩放，增加景深
                    animationSpec = containerVisualPhysics()
                )
            }

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
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            // 教务信息-成绩查询
            composable(AcademicDestinations.Grades.route) {
                GradesScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            // 考试信息查询
            composable(AcademicDestinations.Exams.route) {
                GetExamInfoScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            // 调课通知列表
            composable(AcademicDestinations.Messages.route) {
                GetAcademicMessageInfoScreen(
                    windowSizeClass = windowSizeClass,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            // 测试页面
            composable(AcademicDestinations.Test.route) {
                TestScreen()
            }
        }
    }
}