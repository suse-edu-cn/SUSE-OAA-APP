package com.suseoaa.projectoaa.startHomeNavigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.startHomeNavigation.ui.CompactLayout as AdaptiveCompactLayout
import com.suseoaa.projectoaa.startHomeNavigation.ui.MediumLayout as AdaptiveMediumLayout
import com.suseoaa.projectoaa.startHomeNavigation.ui.ExpandedLayout as AdaptiveExpandedLayout
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
@Composable
fun AdaptiveApp(
    windowSizeClass: WindowWidthSizeClass,
    shareViewModel: ShareViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            AdaptiveCompactLayout(
                navController = navController,
                onLogout = onLogout,
                shareViewModel = shareViewModel
            )
        }
        WindowWidthSizeClass.Medium -> {
            AdaptiveMediumLayout(
                navController = navController,
                onLogout = onLogout,
                shareViewModel = shareViewModel
            )
        }
        WindowWidthSizeClass.Expanded -> {
            AdaptiveExpandedLayout(
                navController = navController,
                onLogout = onLogout,
                shareViewModel = shareViewModel
            )
        }
    }
}