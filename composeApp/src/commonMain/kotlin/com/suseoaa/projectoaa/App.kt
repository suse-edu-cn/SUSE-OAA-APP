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
    val startDestination by mainViewModel.startDestination.collectAsState()
    
    ProjectOAATheme {
        val navController = rememberNavController()
        
        AppNavHost(
            navController = navController,
            startDestination = startDestination
        )
    }
}
