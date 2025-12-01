package com.suseoaa.projectoaa.startHomeNavigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.startHomeNavigation.platform.fold.MediumLayout
import com.suseoaa.projectoaa.startHomeNavigation.platform.pad.ExpandedLayout
import com.suseoaa.projectoaa.startHomeNavigation.platform.phone.CompactLayout
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
@Composable
fun AdaptiveApp(
    windowSizeClass: WindowWidthSizeClass,
    shareViewModel: ShareViewModel
) {
    val navController = rememberNavController()

    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactLayout(
                navController = navController,
                viewModel = shareViewModel
            )
        }
        WindowWidthSizeClass.Medium -> {
            MediumLayout(
                navController = navController,
                viewModel = shareViewModel
            )
        }
        WindowWidthSizeClass.Expanded -> {
            ExpandedLayout(
                navController = navController,
                viewModel = shareViewModel
            )
        }
    }
}