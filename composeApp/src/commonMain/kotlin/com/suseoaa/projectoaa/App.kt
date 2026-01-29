package com.suseoaa.projectoaa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        // 全局 Toast 处理器
        ToastHandler()
        
        // 等待加载完成
        if (startDestination == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = startDestination!!
            )
        }
    }
}
