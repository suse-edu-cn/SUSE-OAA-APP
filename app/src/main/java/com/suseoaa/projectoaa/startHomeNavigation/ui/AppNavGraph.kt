package com.suseoaa.projectoaa.startHomeNavigation.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.courseList.ui.screen.CourseListScreen
import com.suseoaa.projectoaa.courseList.viewmodel.CourseListViewModel
import com.suseoaa.projectoaa.login.ui.ProfileScreen
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import com.suseoaa.projectoaa.student.ui.StudentFormScreen
import com.suseoaa.projectoaa.student.viewmodel.StudentFormViewModel

// ==========================================
// 核心路由图 (AppNavigationGraph)
// ==========================================

/**
 * 包含应用所有路由的 NavHost 组件
 * (已从 AdaptiveLayouts 中解耦)
 */
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    shareViewModel: ShareViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home.route,
        modifier = modifier,
        enterTransition = { getEnterTransition(true) },
        exitTransition = { getExitTransition(true) },
        popEnterTransition = { getEnterTransition(false) },
        popExitTransition = { getExitTransition(false) }
    ) {
        // --- 首页 ---
        composable(route = AppRoutes.Home.route) {
            HomeScreen(
                navController = navController,
                shareViewModel = shareViewModel
            )
        }

        // --- 搜索页 ---
        composable(route = AppRoutes.Search.route) {
            SearchContent(viewModel = shareViewModel)
        }

        // --- 设置页 (主页) ---
        composable(route = AppRoutes.Settings.route) {
            SettingsContent(
                viewModel = shareViewModel,
                navController = navController
            )
        }

        // --- 个人页 ---
        composable(route = AppRoutes.Profile.route) {
            ProfileScreen(onBack = { navController.navigate(AppRoutes.Home.route) }, onLogout = onLogout)
        }

        // --- 新增功能页 ---
        composable(route = AppRoutes.CourseList.route) {
            CourseListScreen(
                viewModel = hiltViewModel<CourseListViewModel>(),
//                onBack = { navController.popBackStack() }
            )
        }
        composable(route = AppRoutes.StudentForm.route) {
            StudentFormScreen(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel<StudentFormViewModel>(),
                currentThemeName = shareViewModel.currentTheme.name
            )
        }

        // --- [修改] 新增：待办事项详情页 ---
        composable(
            route = "todo_detail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            GenericDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // --- 设置子页面 (全部使用共享 shareViewModel) ---
        composable(route = "settings_notifications") {
            NotificationsScreen(
                viewModel = shareViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings_privacy") {
            PrivacyScreen(
                viewModel = shareViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings_about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(route = "settings_feedback") {
            FeedbackScreen(
                viewModel = shareViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings_appearance") {
            AppearanceScreen(
                viewModel = shareViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}