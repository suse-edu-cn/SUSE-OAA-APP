package com.suseoaa.projectoaa.startHomeNavigation.platform.pad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
fun ExpandedLayout(navController: NavHostController, viewModel: ShareViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val homeIndex = 0
    val courseIndex = 1
    val settingsIndex = 2
    val profileIndex = 3

    val livelyItemColors = NavigationDrawerItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(modifier = Modifier.fillMaxSize()) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .width(280.dp)
                        .shadow(elevation = 2.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Project OAA",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("首页") },
                        selected = pagerState.currentPage == homeIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(homeIndex) } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("课表") },
                        selected = pagerState.currentPage == courseIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(courseIndex) } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("设置") },
                        selected = pagerState.currentPage == settingsIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(settingsIndex) } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        colors = livelyItemColors,
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("个人中心") },
                        selected = pagerState.currentPage == profileIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(profileIndex) } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                    // 使用 Pager 包裹主内容
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
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
        }
    }
}