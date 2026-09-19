package com.suseoaa.projectoaa.ui.screen.academic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.common.PullUpFeatureDrawer
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.util.AppPredictiveBackHandler
import kotlin.collections.listOf
import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.ui.component.FeatureCard

// 教务门户首页：功能入口与按屏宽切换的卡片布局。

data class PortalFunction(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicScreen(
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToRescheduling: () -> Unit,
    onNavigateToStudyRequirement: () -> Unit,
    onNavigateToCourseInfo: () -> Unit,
    onNavigateToAcademicStatus: () -> Unit = {},
    featureDrawerExpanded: Boolean = false,
    onFeatureDrawerExpandedChange: (Boolean) -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
    viewModel: AcademicViewModel = koinViewModel()
) {
    val isMainTabVisible = LocalMainTabVisible.current
    val uiState by viewModel.uiState.collectAsState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 错峰加载策略 - 数据为空时自动刷新
    LaunchedEffect(isMainTabVisible) {
        if (isMainTabVisible) {
            kotlinx.coroutines.delay(800)
            if (uiState.exams.isEmpty() || uiState.messages.isEmpty()) {
                viewModel.refresh()
            }
        }
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // 手势进度状态：驱动抽屉实时跟随手势移动
    var backGestureProgress by remember { mutableStateOf<Float?>(null) }
    var backGestureCancelCount by remember { mutableIntStateOf(0) }

    AppPredictiveBackHandler(
        enabled = featureDrawerExpanded,
        onProgress = { event -> backGestureProgress = event.progress },
        onCancel = {
            backGestureProgress = null
            backGestureCancelCount++
        },
        onBack = {
            backGestureProgress = null
            onFeatureDrawerExpandedChange(false)
        }
    )

    val unifiedFunctionColor = MaterialTheme.colorScheme.primary
    val functions = listOf(
        PortalFunction(
            "成绩查询",
            Icons.AutoMirrored.Filled.List,
            "grades",
            MaterialTheme.colorScheme.primary
        ),
        PortalFunction(
            "绩点计算",
            Icons.Default.Star,
            "gpa",
            MaterialTheme.colorScheme.tertiary
        ),
        PortalFunction(
            "修读要求",
            Icons.Default.Menu,
            "studyRequirement",
            MaterialTheme.colorScheme.secondary
        ),
        PortalFunction(
            "课程信息",
            Icons.Default.Info,
            "courseInfo",
            MaterialTheme.colorScheme.error
        ),
        PortalFunction(
            "学业情况",
            Icons.Default.DateRange,
            "academicStatus",
            Color(0xFF9C27B0)
        ),
        PortalFunction(
            "教务系统",
            Icons.AutoMirrored.Filled.ExitToApp,
            "jwgl",
            Color(0xFF1976D2)
        )
    )
    PullUpFeatureDrawer(
        isExpanded = featureDrawerExpanded,
        onExpandedChange = onFeatureDrawerExpandedChange,
        title = "常用功能",
        bottomBarHeight = bottomBarHeight,
        backGestureProgress = backGestureProgress,
        backGestureCancelCount = backGestureCancelCount,
        baseContent = {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() }
            ) {
                AdaptiveLayout { config ->
                    val isTabletLandscape = config.useSideNavigation

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(config.gridColumns),
                        contentPadding = PaddingValues(
                            top = 16.dp + statusBarHeight,
                            bottom = 96.dp + bottomBarHeight,
                            start = config.horizontalPadding,
                            end = config.horizontalPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        if (isTabletLandscape) {
                            item(span = { GridItemSpan(config.gridColumns / 2) }) {
                                TabletReschedulingCard(
                                    messageList = uiState.messages,
                                    onClick = onNavigateToRescheduling,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item(span = { GridItemSpan(config.gridColumns - config.gridColumns / 2) }) {
                                TabletUpcomingExamsCard(
                                    examList = uiState.exams,
                                    onClick = onNavigateToExams,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ReschedulingCard(
                                    messageList = uiState.messages,
                                    onClick = onNavigateToRescheduling
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                UpcomingExamsCard(
                                    examList = uiState.exams,
                                    onClick = onNavigateToExams
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(functions) { func ->
                FeatureCard(
                    name = func.title,
                    icon = func.icon,
                    color = MaterialTheme.colorScheme.surface,
                    onColor = unifiedFunctionColor,
                    onClick = {
                        when (func.route) {
                            "grades" -> onNavigateToGrades()
                            "gpa" -> onNavigateToGpa()
                            "studyRequirement" -> onNavigateToStudyRequirement()
                            "courseInfo" -> onNavigateToCourseInfo()
                            "academicStatus" -> onNavigateToAcademicStatus()
                            "jwgl" -> uriHandler.openUri(ApiConfig.SCHOOL_LOGIN_PAGE)
                        }
                    },
                    sharedBoundKey = func.route
                )
            }
        }
    }
}

/**
 * 功能按钮卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionCard(
    function: PortalFunction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            hoveredElevation = 8.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .sharedBoundsTransition(function.route)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = function.color,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = function.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = function.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
