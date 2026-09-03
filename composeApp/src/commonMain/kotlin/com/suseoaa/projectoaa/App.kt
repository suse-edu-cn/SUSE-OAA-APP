package com.suseoaa.projectoaa

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.navigation.AppNavHost
import com.suseoaa.projectoaa.ui.navigation.Screen
import com.suseoaa.projectoaa.ui.screen.main.MainTab
import com.suseoaa.projectoaa.ui.screen.main.PersistentBottomTabBar
import com.suseoaa.projectoaa.ui.theme.ProjectOAATheme
import com.suseoaa.projectoaa.util.ToastHandler
import dev.chrisbanes.haze.HazeState
import org.koin.compose.viewmodel.koinViewModel

// 亮色渐变
private val LightGradientColors = listOf(
    Color(0xFF9BDCE5),
    Color(0xFF8EC5FC),
)

// 暗色渐变
private val DarkGradientColors = listOf(
    Color(0xFF1A3A4A),
    Color(0xFF1A2A4A),
)

@Composable
fun App(
    mainViewModel: MainViewModel = koinViewModel()
) {
    val destination by mainViewModel.startDestination.collectAsState()
    var initialDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(destination) {
        if (initialDestination == null && destination != null) {
            initialDestination = destination
        }
    }

    val dynamicColorEnabled by mainViewModel.dynamicColorEnabled.collectAsState()
    val dynamicPaletteLightColorHex by mainViewModel.dynamicPaletteLightColorHex.collectAsState()
    val dynamicPaletteDarkColorHex by mainViewModel.dynamicPaletteDarkColorHex.collectAsState()

    // 首页 Tab 切换的分页状态与底部栏共用同一个 HazeState，二者一起从 NavHost 里提出来，
    // 常驻渲染，不再随页面跳转的转场动画被缩放/裁剪（原因见 PersistentBottomTabBar 的注释）。
    val selectedMainTab by mainViewModel.selectedMainTab.collectAsState()
    val isLiquidGlassTabbarEnabled by mainViewModel.isLiquidGlassTabbarEnabled.collectAsState()
    val liquidGlassTabbarStyle by mainViewModel.liquidGlassTabbarStyle.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = selectedMainTab,
        pageCount = { MainTab.entries.size }
    )
    val hazeState = remember { HazeState() }
    var bottomBarHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val bottomBarHeight = with(LocalDensity.current) { bottomBarHeightPx.toDp() }

    ProjectOAATheme(
        dynamicColor = dynamicColorEnabled,
        dynamicPaletteLightColorHex = dynamicPaletteLightColorHex,
        dynamicPaletteDarkColorHex = dynamicPaletteDarkColorHex
    ) {
        AdaptiveLayout(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) { config ->
            // 等待加载完成
            val currentStartDestination = initialDestination
            if (currentStartDestination == null) {
                val isDarkTheme = isSystemInDarkTheme()
                val gradientColors = if (isDarkTheme) DarkGradientColors else LightGradientColors
                val headerTextColor = if (isDarkTheme) Color.White else Color.Black

                // 启动加载界面 - 渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = gradientColors + MaterialTheme.colorScheme.background
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "青蟹",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "致力服务于四川轻化工大学开放原子开源协会",
                            style = MaterialTheme.typography.bodyMedium,
                            color = headerTextColor.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val isOnMainDestination = currentBackStackEntry?.destination?.route == Screen.Main.route

                AppNavHost(
                    navController = navController,
                    startDestination = currentStartDestination,
                    mainViewModel = mainViewModel,
                    pagerState = pagerState,
                    hazeState = hazeState,
                    bottomBarHeight = bottomBarHeight,
                    onBottomBarHeightChanged = { bottomBarHeightPx = it }
                )

                // 平板横屏走侧边导航栏（OaaNavigationRail），不需要这个底部栏。
                PersistentBottomTabBar(
                    visible = isOnMainDestination && !config.useSideNavigation,
                    selectedTab = selectedMainTab,
                    onTabChange = { mainViewModel.updateSelectedMainTab(it) },
                    pagerState = pagerState,
                    hazeState = hazeState,
                    isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
                    liquidGlassTabbarStyle = liquidGlassTabbarStyle,
                    onBottomBarHeightChanged = { bottomBarHeightPx = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // 全局 Toast 处理器 - 放在最上层
            ToastHandler()
        }
    }
}
