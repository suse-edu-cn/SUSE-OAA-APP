package com.suseoaa.projectoaa.common.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.login.ui.LoginScreen
import com.suseoaa.projectoaa.login.ui.ProfileScreen
import com.suseoaa.projectoaa.login.ui.RegisterScreen
import com.suseoaa.projectoaa.login.ui.SplashScreen
import com.suseoaa.projectoaa.login.viewmodel.MainViewModel
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import com.suseoaa.projectoaa.startHomeNavigation.AdaptiveApp
import com.suseoaa.projectoaa.student.ui.StudentAppMainEntry

@Composable
fun AppNavigation(
    windowSizeClass: WindowWidthSizeClass,
    shareViewModel: ShareViewModel
) {

    val navController = rememberNavController()
    val loginViewModel: MainViewModel = hiltViewModel()

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
                shareViewModel = shareViewModel,
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

//        composable(AppRoutes.StudentEntry.route) {
//            StudentAppMainEntry(
//                onLogout = {
//                    loginViewModel.logout()
//                    navController.navigate(AppRoutes.Login.route) {
//                        popUpTo(0) { inclusive = true }
//                        launchSingleTop = true
//                    }
//                }
//            )
//        }
//
//        composable(AppRoutes.Profile.route) {
//            ProfileScreen(
//                onBack = { navController.popBackStack() },
//                onLogout = {
//                    loginViewModel.logout()
//                    navController.navigate(AppRoutes.Login.route) {
//                        popUpTo(0) { inclusive = true }
//                        launchSingleTop = true
//                    }
//                }
//            )
//        }
    }
}