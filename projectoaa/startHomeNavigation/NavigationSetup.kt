package com.suseoaa.projectoaa.startHomeNavigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.startHomeNavigation.platform.fold.MediumLayout
import com.suseoaa.projectoaa.startHomeNavigation.platform.pad.ExpandedLayout
import com.suseoaa.projectoaa.startHomeNavigation.platform.phone.CompactLayout
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveApp(windowSizeClass: WindowWidthSizeClass, viewModel: ShareViewModel) {
    // 使用 NavController 来管理页面路由（navigation-compose 的 NavHost -> 默认无动画瞬切）
    val navController = rememberNavController()

    // 【关键判断】根据屏幕宽度选择不同的布局策略
    when (windowSizeClass) {
        // 【手机】紧凑屏幕（宽度 < 600dp）
        WindowWidthSizeClass.Compact -> {
            CompactLayout(navController, viewModel)
        }

        // 【小平板/横屏手机】中等屏幕（600dp ≤ 宽度 < 840dp）
        WindowWidthSizeClass.Medium -> {
            MediumLayout(navController, viewModel)
        }

        // 【大平板】展开屏幕（宽度 ≥ 840dp）
        WindowWidthSizeClass.Expanded -> {
            ExpandedLayout(navController, viewModel)
        }

        else -> {
            CompactLayout(navController, viewModel)
        }
    }
}