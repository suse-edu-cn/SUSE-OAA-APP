package com.suseoaa.projectoaa.startHomeNavigation.platform.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.suseoaa.projectoaa.startHomeNavigation.ui.CourseContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.HomeContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.ProfileContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.SettingsContent
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLayout(navController: NavHostController, viewModel: ShareViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val homeIndex = 0
    val courseIndex = 1
    val settingsIndex = 2
    val profileIndex = 3

    val livelyItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("首页") },
                        selected = pagerState.currentPage == homeIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(homeIndex) } }
                    )
                    NavigationBarItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("课表") },
                        selected = pagerState.currentPage == courseIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(courseIndex) } }
                    )
                    NavigationBarItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("设置") },
                        selected = pagerState.currentPage == settingsIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(settingsIndex) } }
                    )
                    NavigationBarItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("个人") },
                        selected = pagerState.currentPage == profileIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(profileIndex) } }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                // [核心修复] 不要直接使用 .padding(padding)，因为那包含了顶部状态栏高度。
                // 我们只应用底部的 padding（避开底部导航栏），让顶部内容（Page）可以延伸到状态栏下方。
                // 这样 Page 内部的白色 TopBar 就能覆盖状态栏背景了。
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                homeIndex -> HomeContent(viewModel, navController)
                courseIndex -> CourseContent(viewModel, navController)
                settingsIndex -> SettingsContent(viewModel, navController)
                profileIndex -> ProfileContent(viewModel)
            }
        }
    }
}