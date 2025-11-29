package com.suseoaa.projectoaa.common.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.ui.LoginScreen
import com.suseoaa.projectoaa.login.ui.ProfileScreen
import com.suseoaa.projectoaa.login.ui.RegisterScreen
import com.suseoaa.projectoaa.login.ui.SplashScreen
import com.suseoaa.projectoaa.login.viewmodel.MainViewModel
import com.suseoaa.projectoaa.navigation.AdaptiveApp
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel
import com.suseoaa.projectoaa.student.ui.StudentAppMainEntry

@Composable
fun AppNavigation(windowSizeClass: WindowWidthSizeClass, viewModel: ShareViewModel) {

    val navController = rememberNavController()
    val loginViewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = AppRoutes.Splash.route) {

        composable(AppRoutes.Splash.route) {
            SplashScreen(navController, loginViewModel)
        }

        composable(AppRoutes.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.Register.route) {
            RegisterScreen(navController, loginViewModel)
        }

        composable(AppRoutes.Home.route) {
            AdaptiveApp(
                windowSizeClass = windowSizeClass,
                viewModel = viewModel,
                onLogout = {
                    SessionManager.clear(context)
                    loginViewModel.clearState()
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoutes.StudentEntry.route) {
            StudentAppMainEntry(
                onLogout = {
                    SessionManager.clear(context)
                    loginViewModel.clearState()
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoutes.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    SessionManager.clear(context)
                    loginViewModel.clearState()
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}