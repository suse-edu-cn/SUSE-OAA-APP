package com.suseoaa.projectoaa.startHomeNavigation.platform.fold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.unit.dp
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

// ========== 小平板布局：侧边导航栏 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumLayout(navController: NavHostController, viewModel: ShareViewModel) {
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"

    // 跟踪导航方向
    var isForward by remember { mutableStateOf(true) }

    // 当前路由变化时更新导航方向
    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 【左侧导航栏】紧凑的侧边栏，只显示图标
        NavigationRail(
            modifier = Modifier.fillMaxHeight(), header = {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
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

            NavigationRailItem(
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

            NavigationRailItem(
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
                            ) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })

            NavigationRailItem(
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

        // 【右侧内容区域】直接使用 NavHost（带过渡动画）
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("小平板模式") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
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
}
