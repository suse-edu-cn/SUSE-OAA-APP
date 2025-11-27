package com.suseoaa.projectoaa.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.login.viewmodel.MainViewModel

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.checkToken(context)
    }

    LaunchedEffect(key1 = viewModel.isTokenValid) {
        when (viewModel.isTokenValid) {
            true -> {
                //  Entry
                navController.navigate(AppRoutes.Home.route) {
                    popUpTo(AppRoutes.Splash.route) { inclusive = true }
                }
            }
            false -> {
                // Login
                navController.navigate(AppRoutes.Login.route) {
                    popUpTo(AppRoutes.Splash.route) { inclusive = true }
                }
            }
            else -> {
                // Loading
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Project:OAA",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}