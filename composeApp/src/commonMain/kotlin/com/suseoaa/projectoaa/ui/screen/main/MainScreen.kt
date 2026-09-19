package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.compose.viewmodel.koinViewModel

// 应用主框架：按屏幕形态切换手机/平板布局。

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
    onNavigateToAcademicMessages: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToRecruitment: () -> Unit = {},
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit = {},
    onNavigateToValueCalculator: () -> Unit = {},
    onNavigateToUpdate: () -> Unit, onNavigateToSettings: () -> Unit = {},
    onNavigateToCourseStatistics: () -> Unit = {},
    pagerState: PagerState,
    hazeState: HazeState,
    bottomBarHeight: Dp,
    onBottomBarHeightChanged: (Int) -> Unit,
    mainViewModel: MainViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    // 由 MainViewModel 托管 Tab 状态，确保前景与背景预览层保持同一上下文。
    val selectedTab by mainViewModel.selectedMainTab.collectAsState()
    val homeFeatureDrawerExpanded by mainViewModel.homeFeatureDrawerExpanded.collectAsState()
    val academicFeatureDrawerExpanded by mainViewModel.academicFeatureDrawerExpanded.collectAsState()
    val appBackgroundImages by mainViewModel.appBackgroundImages.collectAsState()
    val defaultStartTab by mainViewModel.defaultStartTab.collectAsState()
    val isLiquidGlassTabbarEnabled by mainViewModel.isLiquidGlassTabbarEnabled.collectAsState()
    val liquidGlassTabbarStyle by mainViewModel.liquidGlassTabbarStyle.collectAsState()

    AdaptiveLayout { config ->
        if (config.useSideNavigation) {
            // 平板横屏：使用侧边导航栏布局
            TabletLandscapeLayout(
                config = config,
                appBackgroundImages = appBackgroundImages,
                selectedTab = selectedTab,
                onTabChange = { mainViewModel.updateSelectedMainTab(it) },
                homeFeatureDrawerExpanded = homeFeatureDrawerExpanded,
                onHomeFeatureDrawerExpandedChange = {
                    mainViewModel.updateHomeFeatureDrawerExpanded(
                        it
                    )
                },
                academicFeatureDrawerExpanded = academicFeatureDrawerExpanded,
                onAcademicFeatureDrawerExpandedChange = {
                    mainViewModel.updateAcademicFeatureDrawerExpanded(it)
                },
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToAcademicMessages = onNavigateToAcademicMessages,
                onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                onNavigateToValueCalculator = onNavigateToValueCalculator,
                onNavigateToUpdate = onNavigateToUpdate, onNavigateToSettings = onNavigateToSettings,
                onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                modifier = modifier
            )
        } else {
            // 手机或平板竖屏：使用底部导航栏布局
            PhoneLayout(
                appBackgroundImages = appBackgroundImages,
                selectedTab = selectedTab,
                initialStartTab = defaultStartTab,
                onTabChange = { mainViewModel.updateSelectedMainTab(it) },
                homeFeatureDrawerExpanded = homeFeatureDrawerExpanded,
                onHomeFeatureDrawerExpandedChange = {
                    mainViewModel.updateHomeFeatureDrawerExpanded(
                        it
                    )
                },
                academicFeatureDrawerExpanded = academicFeatureDrawerExpanded,
                onAcademicFeatureDrawerExpandedChange = {
                    mainViewModel.updateAcademicFeatureDrawerExpanded(it)
                },
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToAcademicMessages = onNavigateToAcademicMessages,
                onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                onNavigateToValueCalculator = onNavigateToValueCalculator,
                onNavigateToUpdate = onNavigateToUpdate, onNavigateToSettings = onNavigateToSettings,
                onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
                liquidGlassTabbarStyle = liquidGlassTabbarStyle,
                pagerState = pagerState,
                hazeState = hazeState,
                bottomBarHeight = bottomBarHeight,
                onBottomBarHeightChanged = onBottomBarHeightChanged,
                modifier = modifier
            )
        }
    }
}

/**
 * 平板横屏布局 - 左侧导航栏 + 右侧内容区
 */
@Composable
private fun TabletLandscapeLayout(
    config: AdaptiveLayoutConfig,
    appBackgroundImages: Map<String, String?>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    homeFeatureDrawerExpanded: Boolean,
    onHomeFeatureDrawerExpandedChange: (Boolean) -> Unit,
    academicFeatureDrawerExpanded: Boolean,
    onAcademicFeatureDrawerExpandedChange: (Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToAcademicMessages: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit,
    onNavigateToValueCalculator: () -> Unit,
    onNavigateToUpdate: () -> Unit, onNavigateToSettings: () -> Unit,
    onNavigateToCourseStatistics: () -> Unit,
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
            MainPageBackground(
                encodedImage = resolveBackgroundImage(appBackgroundImages, selectedTab),
                modifier = Modifier.fillMaxSize()
            ) {
                KeepAliveMainPages(
                    selectedTab = selectedTab,
                    bottomBarHeight = 0.dp,
                    homeFeatureDrawerExpanded = homeFeatureDrawerExpanded,
                    onHomeFeatureDrawerExpandedChange = onHomeFeatureDrawerExpandedChange,
                    academicFeatureDrawerExpanded = academicFeatureDrawerExpanded,
                    onAcademicFeatureDrawerExpandedChange = onAcademicFeatureDrawerExpandedChange,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToChangePassword = onNavigateToChangePassword,
                    onNavigateToGrades = onNavigateToGrades,
                    onNavigateToGpa = onNavigateToGpa,
                    onNavigateToExams = onNavigateToExams,
                    onNavigateToAcademicMessages = onNavigateToAcademicMessages,
                    onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                    onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                    onNavigateToCourseInfo = onNavigateToCourseInfo,
                    onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                    onNavigateToCheckin = onNavigateToCheckin,
                    onNavigateToRecruitment = onNavigateToRecruitment,
                    onNavigateToUserQuery = onNavigateToUserQuery,
                    onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                    onNavigateToValueCalculator = onNavigateToValueCalculator,
                    onNavigateToUpdate = onNavigateToUpdate, onNavigateToSettings = onNavigateToSettings,
                    onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                )
            }
        }
    }
}

/**
 * 手机/平板竖屏布局 - 底部导航栏
 */
@Composable
private fun PhoneLayout(
    appBackgroundImages: Map<String, String?>,
    selectedTab: Int,
    initialStartTab: Int = 0,
    onTabChange: (Int) -> Unit,
    homeFeatureDrawerExpanded: Boolean,
    onHomeFeatureDrawerExpandedChange: (Boolean) -> Unit,
    academicFeatureDrawerExpanded: Boolean,
    onAcademicFeatureDrawerExpandedChange: (Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToAcademicMessages: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToActivityCheckin: () -> Unit,
    onNavigateToValueCalculator: () -> Unit,
    onNavigateToUpdate: () -> Unit, onNavigateToSettings: () -> Unit,
    onNavigateToCourseStatistics: () -> Unit,
    isLiquidGlassTabbarEnabled: Boolean = false,
    liquidGlassTabbarStyle: Int = 1,
    pagerState: PagerState,
    hazeState: HazeState,
    bottomBarHeight: Dp,
    onBottomBarHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        com.suseoaa.projectoaa.ui.component.sukisu.LiquidGlassBackdropWrapper(
            isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
            liquidGlassTabbarStyle = liquidGlassTabbarStyle,
            selectedIndex = { selectedTab },
            onNavigate = onTabChange,
            onBottomBarHeightChanged = onBottomBarHeightChanged,
            modifier = Modifier.fillMaxSize()
        ) { backdropModifier ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(backdropModifier)
                    .hazeSource(state = hazeState),
                beyondViewportPageCount = MainTab.entries.size - 1,
            ) { page ->
                MainPageBackground(
                    encodedImage = resolveBackgroundImage(appBackgroundImages, page),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { clip = true }
                ) {
                MainTabPage(
                    tabIndex = page,
                    isVisible = page == selectedTab,
                    bottomBarHeight = bottomBarHeight,
                    homeFeatureDrawerExpanded = homeFeatureDrawerExpanded,
                    onHomeFeatureDrawerExpandedChange = onHomeFeatureDrawerExpandedChange,
                    academicFeatureDrawerExpanded = academicFeatureDrawerExpanded,
                    onAcademicFeatureDrawerExpandedChange = onAcademicFeatureDrawerExpandedChange,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToChangePassword = onNavigateToChangePassword,
                    onNavigateToGrades = onNavigateToGrades,
                    onNavigateToGpa = onNavigateToGpa,
                    onNavigateToExams = onNavigateToExams,
                    onNavigateToAcademicMessages = onNavigateToAcademicMessages,
                    onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                    onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                    onNavigateToCourseInfo = onNavigateToCourseInfo,
                    onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                    onNavigateToCheckin = onNavigateToCheckin,
                    onNavigateToRecruitment = onNavigateToRecruitment,
                    onNavigateToUserQuery = onNavigateToUserQuery,
                    onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                    onNavigateToValueCalculator = onNavigateToValueCalculator,
                    onNavigateToUpdate = onNavigateToUpdate, onNavigateToSettings = onNavigateToSettings,
                    onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                )
            } // End MainPageBackground
        } // End HorizontalPager
        } // End LiquidGlassBackdropWrapper
        // 底部导航栏已上移到 App() 里的 PersistentBottomTabBar，与 pagerState/hazeState 共享，
        // 不再随本目的地的转场动画一起被缩放/裁剪。
    }
}
