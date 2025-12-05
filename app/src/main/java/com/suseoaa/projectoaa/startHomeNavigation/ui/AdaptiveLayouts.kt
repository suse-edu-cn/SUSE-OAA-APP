package com.suseoaa.projectoaa.startHomeNavigation.ui

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel

// ==========================================
// 核心：导航逻辑封装
// ==========================================
/**
 * 执行 Tab 切换的标准逻辑：
 * 1. popUpTo(findStartDestination): 弹出到起始页，避免返回栈无限增长，实现“平行”效果。
 * 2. saveState = true: 弹出时保存被弹出页面的状态（如 ViewModel、滚动位置）。
 * 3. launchSingleTop = true: 避免重复点击同一个 Tab 时创建多个实例。
 * 4. restoreState = true: 如果之前保存过状态，则恢复状态。
 */
private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        // [关键] 弹出到导航图的起始目的地 (Home)，实现平行层级
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // 避免在栈顶创建同一页面的副本
        launchSingleTop = true
        // [关键] 恢复之前保存的状态
        restoreState = true
    }
}

// ==========================================
// 1. 紧凑布局 (手机竖屏 - 底部导航)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.Home.route

    // 定义哪些页面显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        AppRoutes.Home.route,
        AppRoutes.CourseList.route,
//        AppRoutes.Settings.route,
        AppRoutes.Profile.route
    )

    // 背景与主题逻辑
    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val isAnimeTheme = shareViewModel.currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸渲染
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
                        // 首页
                        NavigationBarItem(
                            icon = { Icon(AppRoutes.Home.icon, null) },
                            label = { Text(AppRoutes.Home.title) },
                            selected = currentRoute == AppRoutes.Home.route,
                            onClick = { navigateToTab(navController, AppRoutes.Home.route) }
                        )
                        // 课表
                        NavigationBarItem(
                            icon = { Icon(AppRoutes.CourseList.icon, null) },
                            label = { Text(AppRoutes.CourseList.title) },
                            selected = currentRoute == AppRoutes.CourseList.route,
                            onClick = { navigateToTab(navController, AppRoutes.CourseList.route) }
                        )
                        // 设置
//                        NavigationBarItem(
//                            icon = { Icon(AppRoutes.Settings.icon, null) },
//                            label = { Text(AppRoutes.Settings.title) },
//                            selected = currentRoute == AppRoutes.Settings.route,
//                            onClick = { navigateToTab(navController, AppRoutes.Settings.route) }
//                        )
                        // 个人
                        NavigationBarItem(
                            icon = { Icon(AppRoutes.Profile.icon, null) },
                            label = { Text(AppRoutes.Profile.title) },
                            selected = currentRoute == AppRoutes.Profile.route,
                            onClick = { navigateToTab(navController, AppRoutes.Profile.route) }
                        )
                    }
                }
            }
        ) { padding ->
            AppNavigationGraph(
                navController = navController,
                shareViewModel = shareViewModel,
                onLogout = onLogout,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    }
}

// ==========================================
// 2. 中等布局 (平板/横屏 - 侧边导航栏)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.Home.route

    val showNavRail = currentRoute in listOf(
        AppRoutes.Home.route,
        AppRoutes.CourseList.route,
//        AppRoutes.Settings.route,
        AppRoutes.Profile.route
    )

    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val isAnimeTheme = shareViewModel.currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        if (isAnimeTheme && wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = maskAlpha))
            )
        } else {
            Box(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (showNavRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    header = {
                        Icon(
                            Icons.Default.Menu,
                            null,
                            Modifier.padding(vertical = 16.dp)
                        )
                    }) {
                    NavigationRailItem(
                        icon = { Icon(AppRoutes.Home.icon, null) },
                        label = { Text(AppRoutes.Home.title) },
                        selected = currentRoute == AppRoutes.Home.route,
                        onClick = { navigateToTab(navController, AppRoutes.Home.route) }
                    )
                    NavigationRailItem(
                        icon = { Icon(AppRoutes.CourseList.icon, null) },
                        label = { Text(AppRoutes.CourseList.title) },
                        selected = currentRoute == AppRoutes.CourseList.route,
                        onClick = { navigateToTab(navController, AppRoutes.CourseList.route) }
                    )
//                    NavigationRailItem(
//                        icon = { Icon(AppRoutes.Settings.icon, null) },
//                        label = { Text(AppRoutes.Settings.title) },
//                        selected = currentRoute == AppRoutes.Settings.route,
//                        onClick = { navigateToTab(navController, AppRoutes.Settings.route) }
//                    )
                    NavigationRailItem(
                        icon = { Icon(AppRoutes.Profile.icon, null) },
                        label = { Text(AppRoutes.Profile.title) },
                        selected = currentRoute == AppRoutes.Profile.route,
                        onClick = { navigateToTab(navController, AppRoutes.Profile.route) }
                    )
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

// ==========================================
// 3. 扩展布局 (大屏 - 永久侧边栏)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedLayout(
    navController: NavHostController,
    onLogout: () -> Unit,
    shareViewModel: ShareViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.Home.route

    val wallpaperUri by shareViewModel.appWallpaper.collectAsStateWithLifecycle()
    val maskAlpha by shareViewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    val isAnimeTheme = shareViewModel.currentTheme.name.contains("二次元")

    Box(modifier = Modifier.fillMaxSize()) {
        if (isAnimeTheme && wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = maskAlpha))
            )
        } else {
            Box(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(280.dp),
                    ) {
                        Spacer(Modifier.height(16.dp))
                        NavigationDrawerItem(
                            icon = { Icon(AppRoutes.Home.icon, null) },
                            label = { Text(AppRoutes.Home.title) },
                            selected = currentRoute == AppRoutes.Home.route,
                            onClick = { navigateToTab(navController, AppRoutes.Home.route) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(AppRoutes.CourseList.icon, null) },
                            label = { Text(AppRoutes.CourseList.title) },
                            selected = currentRoute == AppRoutes.CourseList.route,
                            onClick = { navigateToTab(navController, AppRoutes.CourseList.route) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
//                        NavigationDrawerItem(
//                            icon = { Icon(AppRoutes.Settings.icon, null) },
//                            label = { Text(AppRoutes.Settings.title) },
//                            selected = currentRoute == AppRoutes.Settings.route,
//                            onClick = { navigateToTab(navController, AppRoutes.Settings.route) },
//                            modifier = Modifier.padding(horizontal = 12.dp)
//                        )
                        NavigationDrawerItem(
                            icon = { Icon(AppRoutes.Profile.icon, null) },
                            label = { Text(AppRoutes.Profile.title) },
                            selected = currentRoute == AppRoutes.Profile.route,
                            onClick = { navigateToTab(navController, AppRoutes.Profile.route) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
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