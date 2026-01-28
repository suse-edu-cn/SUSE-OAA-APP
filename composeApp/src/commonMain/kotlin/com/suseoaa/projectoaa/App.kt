package com.suseoaa.projectoaa

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.navigation.AppNavHost
import com.suseoaa.projectoaa.ui.navigation.Screen
import com.suseoaa.projectoaa.ui.theme.ProjectOAATheme
import com.suseoaa.projectoaa.util.ToastHandler
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    mainViewModel: MainViewModel = koinViewModel()
) {
    val startDestination by mainViewModel.startDestination.collectAsState()
    
    ProjectOAATheme {
        val navController = rememberNavController()
        
        // 全局 Toast 处理器
        ToastHandler()
        
        AppNavHost(
            navController = navController,
            startDestination = startDestination
        )
    }
}
