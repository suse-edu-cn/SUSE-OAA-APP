package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.ui.screen.changepassword.ChangePasswordScreen
import com.suseoaa.projectoaa.ui.screen.exam.ExamInfoScreen
import com.suseoaa.projectoaa.ui.screen.gpa.GpaScreen
import com.suseoaa.projectoaa.ui.screen.grades.GradesScreen
import com.suseoaa.projectoaa.ui.screen.home.DepartmentDetailScreen
import com.suseoaa.projectoaa.ui.screen.login.LoginScreen
import com.suseoaa.projectoaa.ui.screen.main.MainScreen
import com.suseoaa.projectoaa.ui.screen.register.RegisterScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // 左右滑动切换动画
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateToGrades = {
                    navController.navigate(Screen.Grades.route)
                },
                onNavigateToGpa = {
                    navController.navigate(Screen.Gpa.route)
                },
                onNavigateToExams = {
                    navController.navigate(Screen.Exams.route)
                },
                onNavigateToDepartmentDetail = { department ->
                    navController.navigate(Screen.DepartmentDetail.createRoute(department))
                }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Grades.route) {
            GradesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Gpa.route) {
            GpaScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Exams.route) {
            ExamInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DepartmentDetail.route,
            arguments = Screen.DepartmentDetail.arguments
        ) { backStackEntry ->
            val department = backStackEntry.arguments?.getString("department") ?: ""
            DepartmentDetailScreen(
                departmentName = department,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
