package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.screen.academic.AcademicScreen
import com.suseoaa.projectoaa.presentation.course.CourseScreen
import com.suseoaa.projectoaa.ui.screen.home.HomeScreen
import com.suseoaa.projectoaa.ui.screen.person.PersonScreen
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.coroutines.launch

// 定义 Tab 的顺序和元数据
enum class MainTab(
    val index: Int,
    val icon: ImageVector,
    val label: String
) {
    HOME(0, Icons.Default.Home, "首页"),
    COURSE(1, Icons.Default.DateRange, "课程"),
    ACADEMIC(2, Icons.AutoMirrored.Filled.List, "教务信息"),
    PERSON(3, Icons.Default.Person, "个人");

    companion object {
        fun getByIndex(index: Int): MainTab = entries.getOrElse(index) { HOME }
    }
}

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用 rememberSaveable 保持 Tab 状态，页面返回时不会丢失
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    AdaptiveLayout { config ->
        if (config.useSideNavigation) {
            // 平板横屏：使用侧边导航栏布局
            TabletLandscapeLayout(
                config = config,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                modifier = modifier
            )
        } else {
            // 手机或平板竖屏：使用底部导航栏布局
            PhoneLayout(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                modifier = modifier
            )
        }
    }
}

/**
 * 平板横屏布局 - 左侧导航栏 + 右侧内容区
 */
@Composable
private fun BoxWithConstraintsScope.TabletLandscapeLayout(
    config: AdaptiveLayoutConfig,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) NightBackground else OxygenBackground
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左侧导航栏 - Card圆角包裹
        OaaNavigationRail(
            selectedIndex = selectedTab,
            onNavigate = onTabChange,
            modifier = Modifier.fillMaxHeight()
        )
        
        // 右侧内容区 - 保持各页面状态，添加圆角
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) NightSurface else OxygenWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // 使用 key 保持状态，而非销毁重建
            when (selectedTab) {
                0 -> key("home") {
                    HomeScreen(
                        onNavigateToDetail = onNavigateToDepartmentDetail,
                        bottomBarHeight = 0.dp
                    )
                }
                1 -> key("course") {
                    CourseScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        bottomBarHeight = 0.dp
                    )
                }
                2 -> key("academic") {
                    AcademicScreen(
                        onNavigateToGrades = onNavigateToGrades,
                        onNavigateToGpa = onNavigateToGpa,
                        onNavigateToExams = onNavigateToExams,
                        onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                        onNavigateToCourseInfo = onNavigateToCourseInfo,
                        bottomBarHeight = 0.dp
                    )
                }
                3 -> key("person") {
                    PersonScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        bottomBarHeight = 0.dp
                    )
                }
            }
        }
    }
}

/**
 * 手机/平板竖屏布局 - 底部导航栏
 */
@Composable
private fun BoxWithConstraintsScope.PhoneLayout(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val currentDestinationIndex by remember { derivedStateOf { pagerState.currentPage } }
    val density = LocalDensity.current
    
    // 同步 pagerState 和 selectedTab
    LaunchedEffect(currentDestinationIndex) {
        if (currentDestinationIndex != selectedTab) {
            onTabChange(currentDestinationIndex)
        }
    }
    
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    
    // 通过测量获取 BottomBar 的实际高度
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeight: Dp = with(density) { bottomBarHeightPx.toDp() }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = true }
            ) {
                when (page) {
                    0 -> HomeScreen(
                        onNavigateToDetail = onNavigateToDepartmentDetail,
                        bottomBarHeight = bottomBarHeight
                    )
                    1 -> CourseScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        bottomBarHeight = bottomBarHeight
                    )
                    2 -> AcademicScreen(
                        onNavigateToGrades = onNavigateToGrades,
                        onNavigateToGpa = onNavigateToGpa,
                        onNavigateToExams = onNavigateToExams,
                        onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                        onNavigateToCourseInfo = onNavigateToCourseInfo,
                        bottomBarHeight = bottomBarHeight
                    )
                    3 -> PersonScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        bottomBarHeight = bottomBarHeight
                    )
                }
            }
        }

        // 底部导航栏 - 测量实际高度
        OaaBottomBar(
            selectedIndex = currentDestinationIndex,
            onNavigate = { index -> 
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { coordinates ->
                    bottomBarHeightPx = coordinates.size.height
                }
        )
    }
}

/**
 * 平板端侧边导航栏 - 圆角Card样式
 */
@Composable
fun OaaNavigationRail(
    selectedIndex: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) NightSurface else OxygenWhite
    val selectedBgColor = if (isDarkTheme) NightContainer else SoftBlueWait
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用 Logo/标题
            Text(
                text = "青蟹",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 28.dp)
            )
            
            // 导航项
            MainTab.entries.forEach { tab ->
                val isSelected = selectedIndex == tab.index
                
                Surface(
                    onClick = { onNavigate(tab.index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) selectedBgColor else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(26.dp),
                            tint = if (isSelected) primaryColor else subtextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else subtextColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// 手机端底部栏
@Composable
fun OaaBottomBar(
    selectedIndex: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedIndex == tab.index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(tab.index) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
