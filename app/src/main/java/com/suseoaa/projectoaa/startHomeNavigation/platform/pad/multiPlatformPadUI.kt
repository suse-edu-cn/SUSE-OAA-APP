package com.suseoaa.projectoaa.startHomeNavigation.platform.pad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

// ========== 大平板布局：双栏布局（主从模式） ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedLayout(
    navController: NavHostController, viewModel: ShareViewModel
) {
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"

    // 跟踪导航方向
    var isForward by remember { mutableStateOf(true) }

    // 当前路由变化时更新导航方向
    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // 【左侧：永久可见的导航抽屉】
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier
                        .width(280.dp)
                        .shadow(
                            elevation = 10.dp,
                            ambientColor = Color.Gray,
                            spotColor = Color.DarkGray
                        )
                ) {
                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    NavigationDrawerItem(
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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.Person, contentDescription = null
                            )
                        },
                        label = { Text("个人中心") },
                        selected = currentRoute == "profile",
                        onClick = {
                            if (currentRoute != "profile") {
                                navController.navigate("profile") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }) {
            // 【中间和右侧：双栏内容区域】
            Row(modifier = Modifier.fillMaxSize()) {
                // 【主内容区域】占 60% 宽度
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    Column {
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
        }
    }
}
