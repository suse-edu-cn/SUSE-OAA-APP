package com.suseoaa.projectoaa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.ui.screen.changepassword.ChangePasswordScreen
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
        startDestination = startDestination
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
            PlaceholderScreen(
                title = "成绩查询",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Gpa.route) {
            PlaceholderScreen(
                title = "GPA 计算",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Exams.route) {
            PlaceholderScreen(
                title = "考试信息",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DepartmentDetail.route,
            arguments = Screen.DepartmentDetail.arguments
        ) { backStackEntry ->
            val department = backStackEntry.arguments?.getString("department") ?: ""
            PlaceholderScreen(
                title = department,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
