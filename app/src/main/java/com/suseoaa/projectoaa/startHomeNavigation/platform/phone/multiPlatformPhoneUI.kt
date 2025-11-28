package com.suseoaa.projectoaa.startHomeNavigation.platform.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.suseoaa.projectoaa.startHomeNavigation.ui.HomeContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.NavigationTracker
import com.suseoaa.projectoaa.startHomeNavigation.ui.ProfileContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.SearchContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.SettingsContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.getEnterTransition
import com.suseoaa.projectoaa.startHomeNavigation.ui.getExitTransition
import com.suseoaa.projectoaa.startHomeNavigation.ui.getNavigationDirection
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel

// ========== 手机布局：底部导航栏 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(navController: NavHostController, viewModel: ShareViewModel) {
    // 当前选中的页面（从 NavController 获取）
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"

    // 跟踪导航方向
    var isForward by remember { mutableStateOf(true) }

    // 当前路由变化时更新导航方向
    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Scaffold(
        // 【顶部栏】
//        topBar = {
//            TopAppBar(
//                title = { Text("手机模式") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor =
//                        MaterialTheme.colorScheme.primaryContainer
//                )
//            )
//        },

        // 【底部导航栏】典型的手机布局
        bottomBar = {
            NavigationBar {
                // 首页
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home, contentDescription = null
                        )
                    },
                    label = { Text("首页") },
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo(
                                    navController.graph.startDestinationId
                                ) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })

                // 搜索
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Search, contentDescription = null
                        )
                    },
                    label = { Text("搜索") },
                    selected = currentRoute == "search",
                    onClick = {
                        if (currentRoute != "search") {
                            navController.navigate("search") {
                                popUpTo(
                                    navController.graph.startDestinationId
                                ) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })

                // 设置
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings, contentDescription = null
                        )
                    },
                    label = { Text("设置") },
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                popUpTo(
                                    navController.graph.startDestinationId
                                )
                                { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })

                // 个人
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Person, contentDescription = null
                        )
                    },
                    label = { Text("个人") },
                    selected = currentRoute == "profile",
                    onClick = {
                        if (currentRoute != "profile") {
                            navController.navigate("profile") {
                                popUpTo(
                                    navController.graph.startDestinationId
                                ) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })
            }
        }) { padding ->
        // 【内容区域】使用 NavHost（带过渡动画），并把 ShareViewModel 传给需要的 screen（满足 MVVM）
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            enterTransition = {
                getEnterTransition(isForward)
            },
            exitTransition = {
                getExitTransition(isForward)
            },
            popEnterTransition = {
                getEnterTransition(isForward)
            },
            popExitTransition = {
                getExitTransition(isForward)
            }
        ) {
            composable(route = "home") { HomeContent(viewModel) }
            composable(route = "search") { SearchContent(viewModel) }
            composable(route = "settings") { SettingsContent(viewModel) }
            composable(route = "profile") { ProfileContent(viewModel) }
        }
    }
}
