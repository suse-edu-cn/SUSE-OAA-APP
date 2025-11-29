package com.suseoaa.projectoaa.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.navigation.ui.CompactLayout
import com.suseoaa.projectoaa.navigation.ui.MediumLayout
import com.suseoaa.projectoaa.navigation.ui.ExpandedLayout
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveApp(
    windowSizeClass: WindowWidthSizeClass,
    viewModel: ShareViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    when (windowSizeClass) {
        // 【手机】紧凑屏幕
        WindowWidthSizeClass.Compact -> {
            CompactLayout(navController, viewModel, onLogout)
        }

        // 【小平板/横屏手机】中等屏幕
        WindowWidthSizeClass.Medium -> {
            MediumLayout(navController, viewModel, onLogout)
        }

        // 【大平板】展开屏幕
        WindowWidthSizeClass.Expanded -> {
            ExpandedLayout(navController, viewModel, onLogout)
        }

        else -> {
            CompactLayout(navController, viewModel, onLogout)
        }
    }
}