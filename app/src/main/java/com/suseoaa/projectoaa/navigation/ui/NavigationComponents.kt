package com.suseoaa.projectoaa.navigation.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState

// [关键引用] 引用正确的 ProfileScreen (含退出功能)
import com.suseoaa.projectoaa.login.ui.ProfileScreen
// [关键引用] 引用正确的 ViewModel (navigation包)
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// ==========================================
// 1. 动画与工具函数
// ==========================================

private val screenOrder = mapOf("home" to 0, "search" to 1, "settings" to 2, "profile" to 3)

fun getNavigationDirection(from: String, to: String): Boolean {
    val fromIndex = screenOrder.getOrDefault(from, 0)
    val toIndex = screenOrder.getOrDefault(to, 0)
    return toIndex > fromIndex
}

fun getEnterTransition(isForward: Boolean): EnterTransition {
    return if (isForward) {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
    } else {
        slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300))
    }
}

fun getExitTransition(isForward: Boolean): ExitTransition {
    return if (isForward) {
        slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = tween(300)) + fadeOut(tween(300))
    } else {
        slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(300)) + fadeOut(tween(300))
    }
}

object NavigationTracker {
    var lastRoute: String = "home"
        private set
    fun updateRoute(newRoute: String) {
        lastRoute = newRoute
    }
}

// ==========================================
// 2. 布局组件 (Compact, Medium, Expanded)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(
    navController: NavHostController,
    viewModel: ShareViewModel,
    onLogout: () -> Unit // [必须] 接收退出回调
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    var isForward by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navigateTo = { route: String ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("首页") }, selected = currentRoute == "home", onClick = { navigateTo("home") })
                NavigationBarItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text("搜索") }, selected = currentRoute == "search", onClick = { navigateTo("search") })
                NavigationBarItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") }, selected = currentRoute == "settings", onClick = { navigateTo("settings") })
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("个人") }, selected = currentRoute == "profile", onClick = { navigateTo("profile") })
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding).fillMaxSize(),
            enterTransition = { getEnterTransition(isForward) },
            exitTransition = { getExitTransition(isForward) },
            popEnterTransition = { getEnterTransition(isForward) },
            popExitTransition = { getExitTransition(isForward) }
        ) {
            composable(route = "home") { HomeContent(viewModel) }
            composable(route = "search") { SearchContent(viewModel) }
            composable(route = "settings") { SettingsContent(viewModel) }
            composable(route = "profile") {
                ProfileScreen(onBack = { navController.navigate("home") }, onLogout = onLogout)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumLayout(
    navController: NavHostController,
    viewModel: ShareViewModel,
    onLogout: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    var isForward by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            header = { Icon(Icons.Default.Menu, null, Modifier.padding(vertical = 16.dp)) }
        ) {
            val navigateTo = { route: String ->
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            NavigationRailItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("首页") }, selected = currentRoute == "home", onClick = { navigateTo("home") })
            NavigationRailItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text("搜索") }, selected = currentRoute == "search", onClick = { navigateTo("search") })
            NavigationRailItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") }, selected = currentRoute == "settings", onClick = { navigateTo("settings") })
            NavigationRailItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("个人") }, selected = currentRoute == "profile", onClick = { navigateTo("profile") })
        }

        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { getEnterTransition(isForward) },
                exitTransition = { getExitTransition(isForward) },
                popEnterTransition = { getEnterTransition(isForward) },
                popExitTransition = { getExitTransition(isForward) }
            ) {
                composable(route = "home") { HomeContent(viewModel) }
                composable(route = "search") { SearchContent(viewModel) }
                composable(route = "settings") { SettingsContent(viewModel) }
                composable(route = "profile") {
                    ProfileScreen(onBack = { navController.navigate("home") }, onLogout = onLogout)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedLayout(
    navController: NavHostController,
    viewModel: ShareViewModel,
    onLogout: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    var isForward by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        isForward = getNavigationDirection(NavigationTracker.lastRoute, currentRoute)
        NavigationTracker.updateRoute(currentRoute)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(280.dp).shadow(10.dp)) {
                    Spacer(Modifier.height(16.dp))
                    val navigateTo = { route: String ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("首页") }, selected = currentRoute == "home", onClick = { navigateTo("home") }, modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text("搜索") }, selected = currentRoute == "search", onClick = { navigateTo("search") }, modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") }, selected = currentRoute == "settings", onClick = { navigateTo("settings") }, modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("个人中心") }, selected = currentRoute == "profile", onClick = { navigateTo("profile") }, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    Column {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = { getEnterTransition(isForward) },
                            exitTransition = { getExitTransition(isForward) },
                            popEnterTransition = { getEnterTransition(isForward) },
                            popExitTransition = { getExitTransition(isForward) }
                        ) {
                            composable(route = "home") { HomeContent(viewModel) }
                            composable(route = "search") { SearchContent(viewModel) }
                            composable(route = "settings") { SettingsContent(viewModel) }
                            composable(route = "profile") {
                                ProfileScreen(onBack = { navController.navigate("home") }, onLogout = onLogout)
                            }
                        }
                    }
                }
            }
        }
    }
}