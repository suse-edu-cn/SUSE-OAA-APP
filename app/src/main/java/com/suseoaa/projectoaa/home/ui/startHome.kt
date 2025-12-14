import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.suseoaa.projectoaa.home.navigation.AppNavigationHost

@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass,
    viewModel: MainViewModel = viewModel() // MVVM 注入
) {
    val navController = rememberNavController()
    // 获取当前的路由堆栈信息，用于高亮当前选中的 Tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 判断是否应该使用侧边栏
    val useNavigationRail = windowSizeClass != WindowWidthSizeClass.Compact

    // 更新 VM 状态
    LaunchedEffect(useNavigationRail) {
        viewModel.updateGreeting(useNavigationRail)
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. 侧边栏 (NavigationRail) - 仅在宽屏显示
            if (useNavigationRail) {
                NavigationRail {
                    topLevelDestinations.forEach { item ->
                        NavigationRailItem(
                            selected = isSelected(currentDestination, item.route),
                            onClick = { navigateToTab(navController, item.route) },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }

            // 2. 主内容区域 + 底部栏
            // 使用 Scaffold 包裹内容，方便放置 BottomBar
            Scaffold(
                bottomBar = {
                    // 3. 底部栏 (NavigationBar) - 仅在窄屏显示
                    if (!useNavigationRail) {
                        NavigationBar {
                            topLevelDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = isSelected(currentDestination, item.route),
                                    onClick = { navigateToTab(navController, item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                // 4. 导航主机
                // 所有的页面切换都在这里发生
                AppNavigationHost(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

// 辅助函数：判断当前 Tab 是否被选中
private fun isSelected(currentDestination: NavDestination?, route: AppRoute): Boolean {
    return currentDestination?.hierarchy?.any { it.route == route.route } == true
}

// 辅助函数：处理 Tab 跳转逻辑
private fun navigateToTab(navController: NavHostController, route: AppRoute) {
    navController.navigate(route.route) {
        // 弹出到导航图的起始目的地，避免在堆栈中积累大量页面
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // 避免同一个 Tab 多次点击产生多个实例
        launchSingleTop = true
        // 切换 Tab 时恢复之前的状态
        restoreState = true
    }
}