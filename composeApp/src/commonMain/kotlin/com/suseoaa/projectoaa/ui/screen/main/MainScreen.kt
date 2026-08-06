package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.foundation.interaction.collectIsPressedAsState

import kotlin.math.pow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.first
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.shared.data.local.BackgroundPageIds
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.screen.academic.AcademicScreen
import com.suseoaa.projectoaa.presentation.course.CourseScreen

import com.suseoaa.projectoaa.ui.screen.home.HomeScreen
import com.suseoaa.projectoaa.ui.screen.person.PersonScreen
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.decodeBackgroundImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
    onNavigateToAiLab: () -> Unit = {},
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
                onNavigateToAiLab = onNavigateToAiLab,
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
                onNavigateToAiLab = onNavigateToAiLab,
                isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
                liquidGlassTabbarStyle = liquidGlassTabbarStyle,
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
    onNavigateToAiLab: () -> Unit = {},
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
                    onNavigateToAiLab = onNavigateToAiLab
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
    onNavigateToAiLab: () -> Unit = {},
    isLiquidGlassTabbarEnabled: Boolean = false,
    liquidGlassTabbarStyle: Int = 1,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var isIndicatorDragging by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var dragIndicatorProgress by remember { mutableStateOf<Float?>(null) }
    val tabIndicatorProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (MainTab.entries.size - 1).toFloat())
        }
    }
    val density = LocalDensity.current
    val hazeState = remember { HazeState() }

    var isFirstComposition by remember { mutableStateOf(true) }

    // 手势滑动完成后，同步 settledPage 到 selectedTab。
    // 程序化跨页动画期间通过检测 scrollJob?.isActive 来避免目标页被中途页覆盖。
    // 使用 isFirstComposition 忽略初始化的首次执行，避免后台恢复时旧的 settledPage 覆盖正确的 selectedTab。
    LaunchedEffect(pagerState.settledPage, isIndicatorDragging) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (!isIndicatorDragging && scrollJob?.isActive != true && pagerState.settledPage != selectedTab) {
            onTabChange(pagerState.settledPage)
        }
    }

    // 若外部或点击产生的 selectedTab 变化，控制 pager 滚动
    LaunchedEffect(selectedTab) {
        val isNotAtTarget = pagerState.currentPage != selectedTab || kotlin.math.abs(pagerState.currentPageOffsetFraction) > 0.001f
        if (!isIndicatorDragging && isNotAtTarget) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                val maxIndex = MainTab.entries.size - 1
                val startProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, maxIndex.toFloat())
                val targetProgress = selectedTab.toFloat().coerceIn(0f, maxIndex.toFloat())
                val distance = kotlin.math.abs(targetProgress - startProgress)
                val durationMillis = (220 + (distance * 140).toInt()).coerceAtMost(680)

                val startNanos = androidx.compose.runtime.withFrameNanos { it }
                var rawFraction = 0f
                while (rawFraction < 1f) {
                    val nowNanos = androidx.compose.runtime.withFrameNanos { it }
                    val elapsedMs = ((nowNanos - startNanos) / 1_000_000f)
                    rawFraction = if (durationMillis <= 0) 1f else (elapsedMs / durationMillis).coerceIn(0f, 1f)
                    val easedFraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(rawFraction)

                    val progress = startProgress + (targetProgress - startProgress) * easedFraction
                    val page = progress.roundToInt().coerceIn(0, maxIndex)
                    val offsetFraction = (progress - page).coerceIn(-0.5f, 0.5f)
                    pagerState.scrollToPage(page = page, pageOffsetFraction = offsetFraction)
                }

                pagerState.scrollToPage(page = selectedTab, pageOffsetFraction = 0f)

                // 这样能够防止因为布局刷新延迟导致的 selectedTab 回退问题
                androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.first { it == selectedTab }
            }
        }
    }

    // 拖拽释放后，保持选中栏停在目标位，直到 Pager 真正滚动到位再释放覆盖状态
    LaunchedEffect(
        pagerState.isScrollInProgress,
        tabIndicatorProgress,
        dragIndicatorProgress,
        isIndicatorDragging
    ) {
        val pinnedProgress = dragIndicatorProgress ?: return@LaunchedEffect
        if (!isIndicatorDragging && !pagerState.isScrollInProgress && kotlin.math.abs(
                tabIndicatorProgress - pinnedProgress
            ) < 0.001f
        ) {
            dragIndicatorProgress = null
        }
    }

    // 拖拽与吸附动画期间，按选中栏进度实时驱动页面位置
    LaunchedEffect(dragIndicatorProgress, isIndicatorDragging) {
        if (!isIndicatorDragging) return@LaunchedEffect
        val progress = dragIndicatorProgress ?: return@LaunchedEffect
        val maxIndex = MainTab.entries.size - 1
        val safeProgress = progress.coerceIn(0f, maxIndex.toFloat())
        val page = safeProgress.roundToInt().coerceIn(0, maxIndex)
        val offsetFraction = (safeProgress - page).coerceIn(-0.5f, 0.5f)
        pagerState.scrollToPage(page = page, pageOffsetFraction = offsetFraction)
    }

    val displayedIndicatorProgress = dragIndicatorProgress ?: tabIndicatorProgress

    // 通过测量获取 BottomBar 的实际高度
    var bottomBarHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val bottomBarHeight: Dp = with(density) { bottomBarHeightPx.toDp() }
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        com.suseoaa.projectoaa.ui.component.sukisu.LiquidGlassBackdropWrapper(
            isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
            liquidGlassTabbarStyle = liquidGlassTabbarStyle,
            selectedIndex = { selectedTab },
            onNavigate = onTabChange,
            onBottomBarHeightChanged = { bottomBarHeightPx = it },
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
                    onNavigateToAiLab = onNavigateToAiLab
                )
            } // End MainPageBackground
        } // End HorizontalPager
        } // End LiquidGlassBackdropWrapper

        // 底部导航栏 - 测量实际高度
        if (!(isLiquidGlassTabbarEnabled && liquidGlassTabbarStyle == 2)) {
            OaaBottomBar(
            selectedIndex = selectedTab, // 直接拿状态驱动按钮
            indicatorProgress = displayedIndicatorProgress,
            onIndicatorDrag = { deltaProgress ->
                val maxProgress = (MainTab.entries.size - 1).toFloat()
                val baseProgress = dragIndicatorProgress ?: tabIndicatorProgress
                val newProgress = (baseProgress + deltaProgress).coerceIn(0f, maxProgress)
                isIndicatorDragging = true
                dragIndicatorProgress = newProgress
            },
            onIndicatorDragEnd = {
                val maxIndex = MainTab.entries.size - 1
                val startProgress = (dragIndicatorProgress ?: tabIndicatorProgress)
                    .coerceIn(0f, maxIndex.toFloat())
                val targetIndex = startProgress.roundToInt().coerceIn(0, maxIndex)
                val targetProgress = targetIndex.toFloat()

                scope.launch {
                    val snapAnim = androidx.compose.animation.core.Animatable(startProgress)
                    snapAnim.animateTo(
                        targetValue = targetProgress,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.82f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) {
                        val animatedProgress = value.coerceIn(0f, maxIndex.toFloat())
                        dragIndicatorProgress = animatedProgress
                    }

                    dragIndicatorProgress = targetProgress
                    isIndicatorDragging = false

                    if (targetIndex != selectedTab) {
                        onTabChange(targetIndex)
                    } else if (!pagerState.isScrollInProgress && kotlin.math.abs(
                            tabIndicatorProgress - targetProgress
                        ) < 0.001f
                    ) {
                        // 已在目标页且对齐完成，立即交回给 Pager 进度驱动
                        dragIndicatorProgress = null
                    }
                }
            },
            onNavigate = { index ->
                isIndicatorDragging = false
                // 点击导航时立即释放拖拽残留覆盖态，避免指示器停留在旧位置。
                dragIndicatorProgress = null
                if (selectedTab != index) {
                    onTabChange(index)
                }
            },
            hazeState = hazeState,
            isLiquidGlassTabbarEnabled = isLiquidGlassTabbarEnabled,
            liquidGlassTabbarStyle = liquidGlassTabbarStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { coordinates ->
                    bottomBarHeightPx = coordinates.size.height
                }
        )
        }
    }
}

@Composable
private fun KeepAliveMainPages(
    selectedTab: Int,
    bottomBarHeight: Dp,
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
    onNavigateToAiLab: () -> Unit = {}
) {
    val orderedTabs = remember(selectedTab) {
        MainTab.entries.sortedBy { if (it.index == selectedTab) 1 else 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        orderedTabs.forEach { tab ->
            // key(tab.index) 确保 Tab 顺序重排时 Compose 移动而非销毁重建 composable，
            // 避免抽屉状态（isInitialized、offsetYAnim 等）在每次切换时被重置
            key(tab.index) {
                val isVisible = tab.index == selectedTab

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (isVisible) drawContent()
                        }
                        .graphicsLayer {
                            alpha = if (isVisible) 1f else 0f
                        }
                ) {
                    MainTabPage(
                        tabIndex = tab.index,
                        isVisible = isVisible,
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
                        onNavigateToAiLab = onNavigateToAiLab
                    )
                }
            } // end key(tab.index)
        }
    }
}

@Composable
private fun MainTabPage(
    tabIndex: Int,
    isVisible: Boolean,
    bottomBarHeight: Dp,
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
    onNavigateToAiLab: () -> Unit = {}
) {
    CompositionLocalProvider(LocalMainTabVisible provides isVisible) {
        when (tabIndex) {
            MainTab.HOME.index -> HomeScreen(
                onNavigateToDetail = onNavigateToDepartmentDetail,
                bottomBarHeight = bottomBarHeight,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                featureDrawerExpanded = homeFeatureDrawerExpanded,
                onFeatureDrawerExpandedChange = onHomeFeatureDrawerExpandedChange,
                onNavigateToActivityCheckin = onNavigateToActivityCheckin,
                onNavigateToValueCalculator = onNavigateToValueCalculator
            )

            MainTab.COURSE.index -> CourseScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToCourseStatistics = onNavigateToCourseStatistics,
                bottomBarHeight = bottomBarHeight
            )

            MainTab.ACADEMIC.index -> AcademicScreen(
                onNavigateToGrades = onNavigateToGrades,
                onNavigateToGpa = onNavigateToGpa,
                onNavigateToExams = onNavigateToExams,
                onNavigateToRescheduling = onNavigateToAcademicMessages,
                onNavigateToStudyRequirement = onNavigateToStudyRequirement,
                onNavigateToCourseInfo = onNavigateToCourseInfo,
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                featureDrawerExpanded = academicFeatureDrawerExpanded,
                onFeatureDrawerExpandedChange = onAcademicFeatureDrawerExpandedChange,
                bottomBarHeight = bottomBarHeight
            )

            MainTab.PERSON.index -> PersonScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToUpdate = onNavigateToUpdate,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAiLab = onNavigateToAiLab,
                bottomBarHeight = bottomBarHeight
            )
        }
    }
}

private fun resolveBackgroundImage(
    appBackgroundImages: Map<String, String?>,
    tabIndex: Int
): String? {
    return if (tabIndex == MainTab.COURSE.index) {
        appBackgroundImages[BackgroundPageIds.COURSE]
    } else {
        null
    }
}

@Composable
private fun MainPageBackground(
    encodedImage: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val imageBytes = remember(encodedImage) { decodeBackgroundImage(encodedImage) }
    val isDarkTheme = isSystemInDarkTheme()
    val scrimAlpha = if (isDarkTheme) 0.38f else 0.24f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (imageBytes != null) {
            AsyncImage(
                model = imageBytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
            )
        }

        content()
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
    val colorScheme = MaterialTheme.colorScheme
    val cardBackgroundColor = colorScheme.surface
    val selectedBgColor = colorScheme.secondaryContainer
    val brandColor = colorScheme.primary
    val selectedContentColor = colorScheme.onSecondaryContainer
    val unselectedContentColor = colorScheme.onSurfaceVariant

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
                color = brandColor,
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
                            tint = if (isSelected) selectedContentColor else unselectedContentColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) selectedContentColor else unselectedContentColor
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
    indicatorProgress: Float,
    onIndicatorDrag: (Float) -> Unit,
    onIndicatorDragEnd: () -> Unit,
    onNavigate: (Int) -> Unit,
    hazeState: HazeState,
    isLiquidGlassTabbarEnabled: Boolean = false,
    liquidGlassTabbarStyle: Int = 1,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedTint = colorScheme.onSecondaryContainer
    val unselectedTint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    
    val indicatorColor = colorScheme.secondaryContainer.copy(alpha = 0.95f)
    val hazeSurface = colorScheme.surfaceColorAtElevation(3.dp)
    
    // 液态玻璃风格切换
    val hazeBackground = if (isLiquidGlassTabbarEnabled) Color.White.copy(alpha = 0.35f) else hazeSurface.copy(alpha = 0.58f)
    val hazeTintColor = if (isLiquidGlassTabbarEnabled) Color.White.copy(alpha = 0.15f) else hazeSurface.copy(alpha = 0.86f)
    val blurRadius = if (isLiquidGlassTabbarEnabled) 48.dp else 28.dp
    val outlineColor = if (isLiquidGlassTabbarEnabled) colorScheme.outlineVariant.copy(alpha = 0.35f) else colorScheme.outlineVariant.copy(alpha = 0.8f)
    val barOverlay = if (isLiquidGlassTabbarEnabled) Color.Transparent else colorScheme.surface.copy(alpha = 0.82f)



    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 48.dp, end = 48.dp, top = 12.dp, bottom = 24.dp)
    ) {
        // 1. 底层 Tabbar 背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (!isLiquidGlassTabbarEnabled) Modifier.shadow(8.dp, RoundedCornerShape(36.dp)) else Modifier
                )
                .clip(RoundedCornerShape(36.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = hazeBackground,
                        tint = HazeTint(hazeTintColor),
                        blurRadius = blurRadius,
                        noiseFactor = 0f
                    )
                )
                .background(barOverlay)
                .border(
                    width = 1.dp,
                    color = outlineColor,
                    shape = RoundedCornerShape(36.dp)
                )
        )

        // 顶层内容容器
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabCount = MainTab.entries.size
            val barHorizontalPadding = 6.dp
            val barVerticalPadding = 4.dp
            val itemSpacing = 2.dp
            val safeProgress = indicatorProgress.coerceIn(0f, (tabCount - 1).toFloat())
            val itemWidth =
                (maxWidth - barHorizontalPadding * 2 - itemSpacing * (tabCount - 1)) / tabCount
            val density = LocalDensity.current
            val dragStepPx = with(density) { (itemWidth + itemSpacing).toPx() }
            val itemWidthPx = with(density) { itemWidth.toPx() }
            
            val indicatorDraggableState = rememberDraggableState { deltaPx ->
                if (dragStepPx > 0f) {
                    onIndicatorDrag(deltaPx / dragStepPx)
                }
            }

            // 状态机：是否正在过渡或按压
            val isTransitioning = kotlin.math.abs(safeProgress - selectedIndex) > 0.01f
            // 为每个Tab创建一个 interactionSource
            val tabInteractionSources = remember { List(tabCount) { androidx.compose.foundation.interaction.MutableInteractionSource() } }
            // 收集所有Tab的按压状态
            val pressedStates = tabInteractionSources.map { it.collectIsPressedAsState() }
            val anyTabPressed = pressedStates.any { it.value }
            
            val isExpanded = isLiquidGlassTabbarEnabled && (isTransitioning || anyTabPressed)

            // 动画过渡气泡高度
            val targetBubbleHeight = if (isExpanded) 84.dp else 52.dp
            val animatedBubbleHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = targetBubbleHeight,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                )
            )

            // 横向液态拉伸 (Liquid Stretch) 算法
            val startTab = kotlin.math.floor(safeProgress.toDouble()).toFloat()
            val f = safeProgress - startTab
            val pow = if (isLiquidGlassTabbarEnabled) 2.4f else 1.2f
            
            val leftProgress = startTab + f.toDouble().pow(pow.toDouble()).toFloat()
            val rightProgress = startTab + f.toDouble().pow((1f / pow).toDouble()).toFloat()
            
            val leftPx = leftProgress * dragStepPx
            val rightPx = rightProgress * dragStepPx + itemWidthPx
            
            val currentIndicatorWidth = with(density) { (rightPx - leftPx).toDp() }
            val currentIndicatorOffset = with(density) { leftPx.toDp() } + barHorizontalPadding

            // 核心渲染区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = barVerticalPadding)
                    .height(60.dp)
                    .draggable(
                        state = indicatorDraggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = { onIndicatorDragEnd() }
                    )
            ) {
                // 计算气泡的垂直偏移（严格数学居中，确保静置和按压时上下对称）
                val bubbleOffsetY = (60.dp - animatedBubbleHeight) / 2

                // 玻璃气泡透镜（透镜的背景）
                val indicatorModifier = if (isLiquidGlassTabbarEnabled) {
                    Modifier
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = indicatorColor.copy(alpha = 0.4f),
                                tint = HazeTint(indicatorColor.copy(alpha = 0.45f)),
                                blurRadius = 64.dp,
                                noiseFactor = 0f
                            )
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f), // Top highlight
                                    indicatorColor.copy(alpha = 0.5f), // Gray body
                                    Color.White.copy(alpha = 0.1f) // Bottom reflection
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                } else {
                    Modifier.background(indicatorColor)
                }

                val indicatorBorder = if (isLiquidGlassTabbarEnabled) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(percent = 50)
                    )
                } else Modifier

                val bubbleShape = RoundedCornerShape(percent = 50)

                // 2. 绘制气泡本体 (Bubble Lens - drawn behind icons)
                Box(
                    modifier = Modifier
                        .offset(x = currentIndicatorOffset, y = bubbleOffsetY)
                        .width(currentIndicatorWidth)
                        .height(animatedBubbleHeight)
                        .clip(bubbleShape)
                        .then(indicatorModifier)
                        .then(indicatorBorder)
                )

                // 3. Normal Icons Layer (Drawn on top of the bubble)
                // 此时整个Row会使用liquidGlassDistortion着色器，进行像素级别的边缘畸变
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = barHorizontalPadding)
                        .then(
                            if (isLiquidGlassTabbarEnabled) {
                                Modifier.liquidGlassDistortion(
                                    isExpanded = isExpanded,
                                    centerX = with(density) { (currentIndicatorOffset + currentIndicatorWidth / 2f).toPx() },
                                    centerY = with(density) { (bubbleOffsetY + animatedBubbleHeight / 2f).toPx() },
                                    width = with(density) { currentIndicatorWidth.toPx() },
                                    height = with(density) { animatedBubbleHeight.toPx() },
                                    fallbackScaleX = 1f + ((rightPx - leftPx) / itemWidthPx - 1f) * 0.15f + 0.1f,
                                    fallbackScaleY = 1.15f,
                                    fallbackPivotX = (with(density) { barHorizontalPadding.toPx() } + leftPx + (rightPx - leftPx) / 2f) / with(density) { this@BoxWithConstraints.maxWidth.toPx() },
                                    fallbackPivotY = 0.5f
                                )
                            } else Modifier
                        ),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainTab.entries.forEachIndexed { index, tab ->
                        val isSelected = selectedIndex == index
                        val iconTint = if (isSelected) selectedTint else unselectedTint
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable(
                                    interactionSource = tabInteractionSources[index],
                                    indication = null,
                                    onClick = { onNavigate(tab.index) }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = iconTint
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = iconTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
