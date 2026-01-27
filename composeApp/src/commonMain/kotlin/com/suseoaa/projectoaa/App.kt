package com.suseoaa.projectoaa

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.navigation.AppNavHost
import com.suseoaa.projectoaa.ui.navigation.Screen
import com.suseoaa.projectoaa.ui.theme.ProjectOAATheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    mainViewModel: MainViewModel = koinViewModel()
) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    
    ProjectOAATheme {
        val navController = rememberNavController()
        
        // 根据登录状态决定起始页面
        val startDestination = if (isLoggedIn) {
            Screen.Main.route
        } else {
            Screen.Login.route
        }
        
        AppNavHost(
            navController = navController,
            startDestination = startDestination
        )
    }
}
