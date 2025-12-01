package com.suseoaa.projectoaa.startHomeNavigation.platform.fold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.suseoaa.projectoaa.startHomeNavigation.ui.HomeContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.ProfileContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.SearchContent
import com.suseoaa.projectoaa.startHomeNavigation.ui.SettingsContent
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumLayout(navController: NavHostController, viewModel: ShareViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val homeIndex = 0
    val searchIndex = 1
    val settingsIndex = 2
    val profileIndex = 3

    val livelyItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            header = {
                Icon(
                    Icons.Default.Menu,
                    null,
                    modifier = Modifier.padding(vertical = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        ) {
            NavigationRailItem(
                colors = livelyItemColors,
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("首页") },
                selected = pagerState.currentPage == homeIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(homeIndex) } }
            )
            NavigationRailItem(
                colors = livelyItemColors,
                icon = { Icon(Icons.Default.Search, null) },
                label = { Text("搜索") },
                selected = pagerState.currentPage == searchIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(searchIndex) } }
            )
            NavigationRailItem(
                colors = livelyItemColors,
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("设置") },
                selected = pagerState.currentPage == settingsIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(settingsIndex) } }
            )
            NavigationRailItem(
                colors = livelyItemColors,
                icon = { Icon(Icons.Default.Person, null) },
                label = { Text("个人") },
                selected = pagerState.currentPage == profileIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(profileIndex) } }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Project OAA") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
            // 同样替换为 HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    homeIndex -> HomeContent(viewModel)
                    searchIndex -> SearchContent(viewModel)
                    settingsIndex -> SettingsContent(viewModel)
                    profileIndex -> ProfileContent(viewModel)
                }
            }
        }
    }
}