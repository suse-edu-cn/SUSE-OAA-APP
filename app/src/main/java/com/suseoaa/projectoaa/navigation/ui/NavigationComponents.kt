package com.suseoaa.projectoaa.navigation.ui

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.login.ui.ProfileScreen
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
fun getEnterTransition(isForward: Boolean): EnterTransition { return slideInHorizontally(initialOffsetX = { if (isForward) it else -it }, animationSpec = tween(300)) + fadeIn(tween(300)) }
fun getExitTransition(isForward: Boolean): ExitTransition { return slideOutHorizontally(targetOffsetX = { if (isForward) -it / 2 else it / 2 }, animationSpec = tween(300)) + fadeOut(tween(300)) }
object NavigationTracker { var lastRoute: String = "home"; fun updateRoute(newRoute: String) { lastRoute = newRoute } }

// ==========================================
// 2. NavHost
// ==========================================
@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: ShareViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onRefreshWallpaper = remember { { ctx: Context -> WallpaperManager.refreshWallpaper(ctx) } }
    val onSaveWallpaper = remember { { ctx: Context -> WallpaperManager.saveCurrentToGallery(ctx) } }
    val onThemeSelected = remember(viewModel) { { theme: OaaThemeConfig -> viewModel.updateTheme(theme) } }
    val onAppNotificationToggle = remember(viewModel) { { enabled: Boolean -> viewModel.onNotificationToggleChanged(enabled) } }
    val onPrivacyToggle = remember(viewModel) { { enabled: Boolean -> viewModel.onPrivacyToggleChanged(enabled) } }
    val onFeedbackTextChanged = remember(viewModel) { { text: String -> viewModel.onFeedbackTextChanged(text) } }
    val onSubmitFeedback = remember(viewModel) { { ctx: Context -> viewModel.submitFeedback(ctx) } }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
        enterTransition = { getEnterTransition(true) },
        exitTransition = { getExitTransition(true) },
        popEnterTransition = { getEnterTransition(false) },
        popExitTransition = { getExitTransition(false) }
    ) {
        // [修改] 传递原始值和稳定的 lambda，而不是整个 viewModel
        composable(route = "home") {
            HomeContent(
                currentThemeName = viewModel.currentTheme.name,
                onRefreshWallpaper = onRefreshWallpaper,
                onSaveWallpaper = onSaveWallpaper
            )
        }

        // SearchContent 暂时不依赖 VM 里的状态，先保留
        composable(route = "search") { SearchContent(viewModel) }

        composable(route = "settings") {
            SettingsContent(
                currentTheme = viewModel.currentTheme,
                notificationEnabled = viewModel.notificationEnabled,
                onThemeSelected = onThemeSelected,
                onSaveWallpaper = onSaveWallpaper,
                navController = navController
            )
        }

        composable(route = "profile") {
            ProfileScreen(onBack = { navController.navigate("home") }, onLogout = onLogout)
        }

        composable(route = "settings_notifications") {
            NotificationsScreen(
                isAppNotificationEnabled = viewModel.notificationEnabled,
                onAppNotificationToggle = onAppNotificationToggle,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings_privacy") {
            PrivacyScreen(
                isPrivacyEnabled = viewModel.privacyEnabled,
                onPrivacyToggle = onPrivacyToggle,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings_about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(route = "settings_feedback") {
            FeedbackScreen(
                feedbackText = viewModel.feedbackText,
                isSubmitting = viewModel.isSubmittingFeedback,
                onFeedbackTextChanged = onFeedbackTextChanged,
                onSubmit = onSubmitFeedback,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ==========================================
// 3. 布局组件
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(
    navController: NavHostController,
    viewModel: ShareViewModel,
    onLogout: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val showBottomBar = currentRoute in listOf("home", "search", "settings", "profile")

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
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
        }
    ) { padding ->
        AppNavHost(
            navController = navController,
            viewModel = viewModel,
            onLogout = onLogout,
            modifier = Modifier.padding(padding).fillMaxSize()
        )
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
    val showNavRail = currentRoute in listOf("home", "search", "settings", "profile")

    Row(modifier = Modifier.fillMaxSize()) {
        if (showNavRail) {
            NavigationRail(modifier = Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), header = { Icon(Icons.Default.Menu, null, Modifier.padding(vertical = 16.dp)) }) {
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
        }
        AppNavHost(
            navController = navController,
            viewModel = viewModel,
            onLogout = onLogout,
            modifier = Modifier.fillMaxSize()
        )
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
            AppNavHost(
                navController = navController,
                viewModel = viewModel,
                onLogout = onLogout,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}