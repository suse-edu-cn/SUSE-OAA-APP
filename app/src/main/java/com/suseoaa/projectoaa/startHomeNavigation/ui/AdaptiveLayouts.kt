package com.suseoaa.projectoaa.navigation.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// ==========================================
// 1. 布局组件 (Compact, Medium, Expanded)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: AppRoutes.Home.route
    val showBottomBar = currentRoute in listOf(AppRoutes.Home.route, AppRoutes.Search.route, AppRoutes.Settings.route, AppRoutes.Profile.route)

    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val currentTheme = shareViewModel.currentTheme
    val isAnimeTheme = currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        // (背景壁纸逻辑...)
        if (isAnimeTheme && wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "App Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = maskAlpha))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                        // (NavigationBarItem 逻辑...)
                        val navigateTo = { route: String ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        NavigationBarItem(icon = { Icon(AppRoutes.Home.icon, null) }, label = { Text(AppRoutes.Home.title) }, selected = currentRoute == AppRoutes.Home.route, onClick = { navigateTo(AppRoutes.Home.route) })
                        NavigationBarItem(icon = { Icon(AppRoutes.Search.icon, null) }, label = { Text(AppRoutes.Search.title) }, selected = currentRoute == AppRoutes.Search.route, onClick = { navigateTo(AppRoutes.Search.route) })
                        NavigationBarItem(icon = { Icon(AppRoutes.Settings.icon, null) }, label = { Text(AppRoutes.Settings.title) }, selected = currentRoute == AppRoutes.Settings.route, onClick = { navigateTo(AppRoutes.Settings.route) })
                        NavigationBarItem(icon = { Icon(AppRoutes.Profile.icon, null) }, label = { Text(AppRoutes.Profile.title) }, selected = currentRoute == AppRoutes.Profile.route, onClick = { navigateTo(AppRoutes.Profile.route) })
                    }
                }
            }
        ) { padding ->
            AppNavigationGraph(
                navController = navController,
                shareViewModel = shareViewModel,
                onLogout = onLogout,
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: AppRoutes.Home.route
    val showNavRail = currentRoute in listOf(AppRoutes.Home.route, AppRoutes.Search.route, AppRoutes.Settings.route, AppRoutes.Profile.route)

    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val currentTheme = shareViewModel.currentTheme
    val isAnimeTheme = currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        // (背景壁纸逻辑...)
        if (isAnimeTheme && wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "App Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = maskAlpha)))
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (showNavRail) {
                NavigationRail(modifier = Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), header = { Icon(Icons.Default.Menu, null, Modifier.padding(vertical = 16.dp)) }) {
                    // (NavigationRailItem 逻辑...)
                    val navigateTo = { route: String ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    NavigationRailItem(icon = { Icon(AppRoutes.Home.icon, null) }, label = { Text(AppRoutes.Home.title) }, selected = currentRoute == AppRoutes.Home.route, onClick = { navigateTo(AppRoutes.Home.route) })
                    NavigationRailItem(icon = { Icon(AppRoutes.Search.icon, null) }, label = { Text(AppRoutes.Search.title) }, selected = currentRoute == AppRoutes.Search.route, onClick = { navigateTo(AppRoutes.Search.route) })
                    NavigationRailItem(icon = { Icon(AppRoutes.Settings.icon, null) }, label = { Text(AppRoutes.Settings.title) }, selected = currentRoute == AppRoutes.Settings.route, onClick = { navigateTo(AppRoutes.Settings.route) })
                    NavigationRailItem(icon = { Icon(AppRoutes.Profile.icon, null) }, label = { Text(AppRoutes.Profile.title) }, selected = currentRoute == AppRoutes.Profile.route, onClick = { navigateTo(AppRoutes.Profile.route) })
                }
            }
            AppNavigationGraph(
                navController = navController,
                shareViewModel = shareViewModel,
                onLogout = onLogout,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: AppRoutes.Home.route

    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val currentTheme = shareViewModel.currentTheme
    val isAnimeTheme = currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        // (背景壁纸逻辑...)
        if (isAnimeTheme && wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "App Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = maskAlpha)))
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(280.dp).shadow(10.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        // (NavigationDrawerItem 逻辑...)
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
                        NavigationDrawerItem(icon = { Icon(AppRoutes.Home.icon, null) }, label = { Text(AppRoutes.Home.title) }, selected = currentRoute == AppRoutes.Home.route, onClick = { navigateTo(AppRoutes.Home.route) }, modifier = Modifier.padding(horizontal = 12.dp))
                        NavigationDrawerItem(icon = { Icon(AppRoutes.Search.icon, null) }, label = { Text(AppRoutes.Search.title) }, selected = currentRoute == AppRoutes.Search.route, onClick = { navigateTo(AppRoutes.Search.route) }, modifier = Modifier.padding(horizontal = 12.dp))
                        NavigationDrawerItem(icon = { Icon(AppRoutes.Settings.icon, null) }, label = { Text(AppRoutes.Settings.title) }, selected = currentRoute == AppRoutes.Settings.route, onClick = { navigateTo(AppRoutes.Settings.route) }, modifier = Modifier.padding(horizontal = 12.dp))
                        NavigationDrawerItem(icon = { Icon(AppRoutes.Profile.icon, null) }, label = { Text(AppRoutes.Profile.title) }, selected = currentRoute == AppRoutes.Profile.route, onClick = { navigateTo(AppRoutes.Profile.route) }, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            ) {
                AppNavigationGraph(
                    navController = navController,
                    shareViewModel = shareViewModel,
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}