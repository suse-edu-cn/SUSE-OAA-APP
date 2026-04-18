package com.suseoaa.projectoaa.ui.screen.main

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.shared.data.local.BackgroundPageIds
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
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
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToRecruitment: () -> Unit = {},
    onNavigateToUserQuery: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    mainViewModel: MainViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    // 使用 rememberSaveable 保持 Tab 状态，页面返回时不会丢失
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val appBackgroundImages by mainViewModel.appBackgroundImages.collectAsState()

    AdaptiveLayout { config ->
        if (config.useSideNavigation) {
            // 平板横屏：使用侧边导航栏布局
            TabletLandscapeLayout(
                config = config,
                appBackgroundImages = appBackgroundImages,
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
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                onNavigateToUpdate = onNavigateToUpdate,
                modifier = modifier
            )
        } else {
            // 手机或平板竖屏：使用底部导航栏布局
            PhoneLayout(
                appBackgroundImages = appBackgroundImages,
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
                onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                onNavigateToCheckin = onNavigateToCheckin,
                onNavigateToRecruitment = onNavigateToRecruitment,
                onNavigateToUserQuery = onNavigateToUserQuery,
                onNavigateToUpdate = onNavigateToUpdate,
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
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToUpdate: () -> Unit,
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
                // 使用 key 保持状态，而非销毁重建
                when (selectedTab) {
                    0 -> key("home") {
                        HomeScreen(
                            onNavigateToDetail = onNavigateToDepartmentDetail,
                            bottomBarHeight = 0.dp,
                            onNavigateToRecruitment = onNavigateToRecruitment,
                            onNavigateToUserQuery = onNavigateToUserQuery
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
                            onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                            bottomBarHeight = 0.dp
                        )
                    }

                    3 -> key("person") {
                        PersonScreen(
                            onNavigateToLogin = onNavigateToLogin,
                            onNavigateToChangePassword = onNavigateToChangePassword,
                            onNavigateToCheckin = onNavigateToCheckin,
                            onNavigateToUpdate = onNavigateToUpdate,
                            bottomBarHeight = 0.dp
                        )
                    }
                }
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
    onTabChange: (Int) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToDepartmentDetail: (String) -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit,
    onNavigateToCheckin: () -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var isIndicatorDragging by remember { mutableStateOf(false) }
    var isProgrammaticTabTransition by remember { mutableStateOf(false) }
    var dragIndicatorProgress by remember { mutableStateOf<Float?>(null) }
    val tabIndicatorProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (MainTab.entries.size - 1).toFloat())
        }
    }
    val density = LocalDensity.current
    val hazeState = remember { HazeState() }

    // 手势滑动完成后，同步 settledPage 到 selectedTab。
    // 程序化跨页动画期间忽略中间页回调，避免目标页被中途页覆盖。
    LaunchedEffect(pagerState.settledPage, isProgrammaticTabTransition, isIndicatorDragging) {
        if (!isIndicatorDragging && !isProgrammaticTabTransition && pagerState.settledPage != selectedTab) {
            onTabChange(pagerState.settledPage)
        }
    }

    // 若外部或点击产生的 selectedTab 变化，控制 pager 滚动
    LaunchedEffect(selectedTab) {
        if (!isIndicatorDragging && pagerState.currentPage != selectedTab) {
            isProgrammaticTabTransition = true
            try {
                val maxIndex = MainTab.entries.size - 1
                val startProgress =
                    (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, maxIndex.toFloat())
                val targetProgress = selectedTab.toFloat().coerceIn(0f, maxIndex.toFloat())
                val distance = kotlin.math.abs(targetProgress - startProgress)
                val durationMillis = (220 + (distance * 140).toInt()).coerceAtMost(680)

                val startNanos = withFrameNanos { it }
                var rawFraction = 0f
                while (rawFraction < 1f) {
                    val nowNanos = withFrameNanos { it }
                    val elapsedMs = ((nowNanos - startNanos) / 1_000_000f)
                    rawFraction =
                        if (durationMillis <= 0) 1f else (elapsedMs / durationMillis).coerceIn(
                            0f,
                            1f
                        )
                    val easedFraction =
                        androidx.compose.animation.core.FastOutSlowInEasing.transform(rawFraction)

                    val progress = startProgress + (targetProgress - startProgress) * easedFraction
                    val page = progress.roundToInt().coerceIn(0, maxIndex)
                    val offsetFraction = (progress - page).coerceIn(-0.5f, 0.5f)
                    pagerState.scrollToPage(page = page, pageOffsetFraction = offsetFraction)
                }

                pagerState.scrollToPage(page = selectedTab, pageOffsetFraction = 0f)
            } finally {
                isProgrammaticTabTransition = false
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

    // 通过测量获取 BottomBar 的实际高度
    var bottomBarHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val bottomBarHeight: Dp = with(density) { bottomBarHeightPx.toDp() }
    val displayedIndicatorProgress = dragIndicatorProgress ?: tabIndicatorProgress

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            beyondViewportPageCount = 2,
        ) { page ->
            MainPageBackground(
                encodedImage = resolveBackgroundImage(appBackgroundImages, page),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = true }
            ) {
                when (page) {
                    0 -> HomeScreen(
                        onNavigateToDetail = onNavigateToDepartmentDetail,
                        bottomBarHeight = bottomBarHeight,
                        onNavigateToRecruitment = onNavigateToRecruitment,
                        onNavigateToUserQuery = onNavigateToUserQuery
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
                        onNavigateToAcademicStatus = onNavigateToAcademicStatus,
                        bottomBarHeight = bottomBarHeight
                    )

                    3 -> PersonScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        onNavigateToCheckin = onNavigateToCheckin,
                        onNavigateToUpdate = onNavigateToUpdate,
                        bottomBarHeight = bottomBarHeight
                    )
                }
            }
        }

        // 底部导航栏 - 测量实际高度
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { coordinates ->
                    bottomBarHeightPx = coordinates.size.height
                }
        )
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
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedTint = colorScheme.onSecondaryContainer
    val unselectedTint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val barOverlay = colorScheme.surface.copy(alpha = 0.82f)
    val indicatorColor = colorScheme.secondaryContainer.copy(alpha = 0.95f)
    val hazeSurface = colorScheme.surfaceColorAtElevation(3.dp)
    val hazeBackground = hazeSurface.copy(alpha = 0.58f)
    val hazeTintColor = hazeSurface.copy(alpha = 0.86f)
    val outlineColor = colorScheme.outlineVariant.copy(alpha = 0.8f)

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 48.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(36.dp))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = hazeBackground,
                    tint = HazeTint(hazeTintColor),
                    blurRadius = 28.dp,
                    noiseFactor = 0f
                )
            )
            .border(
                width = 1.dp,
                color = outlineColor,
                shape = RoundedCornerShape(36.dp)
            ),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(36.dp),
        color = Color.Transparent
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(barOverlay)
        ) {
            val tabCount = MainTab.entries.size
            val barHorizontalPadding = 6.dp
            val barVerticalPadding = 4.dp
            val itemSpacing = 2.dp
            val safeProgress = indicatorProgress.coerceIn(0f, (tabCount - 1).toFloat())
            val itemWidth =
                (maxWidth - barHorizontalPadding * 2 - itemSpacing * (tabCount - 1)) / tabCount
            val indicatorOffset = barHorizontalPadding + (itemWidth + itemSpacing) * safeProgress
            val density = LocalDensity.current
            val dragStepPx = with(density) { (itemWidth + itemSpacing).toPx() }
            val indicatorDraggableState = rememberDraggableState { deltaPx ->
                if (dragStepPx > 0f) {
                    onIndicatorDrag(deltaPx / dragStepPx)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = barVerticalPadding)
                    .height(60.dp)
                    .draggable(
                        state = indicatorDraggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            onIndicatorDragEnd()
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(indicatorColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = barHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainTab.entries.forEach { tab ->
                        val isSelected = selectedIndex == tab.index
                        val interactionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = interactionSource,
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
                                    tint = if (isSelected) selectedTint else unselectedTint
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) selectedTint else unselectedTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
