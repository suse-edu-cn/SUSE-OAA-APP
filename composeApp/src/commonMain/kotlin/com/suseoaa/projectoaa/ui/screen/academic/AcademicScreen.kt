package com.suseoaa.projectoaa.ui.screen.academic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.shared.data.repository.MessageCacheEntity
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.presentation.academic.ExamUiState
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.common.PullUpFeatureDrawer
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.screen.home.FeatureCard
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.getExamCountDown
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.util.AppPredictiveBackHandler
import kotlin.collections.listOf

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
                            "jwgl" -> uriHandler.openUri("https://jwgl.suse.edu.cn/xtgl/login_slogin.html")
                        }
                    },
                    sharedBoundKey = func.route
                )
            }
        }
    }
}

// 平板卡片固定高度常量（基于4条考试信息的高度）
private val TABLET_CARD_HEIGHT = 340.dp

/**
 * 平板端调课信息卡片 - 固定高度，显示最新2条
 */
@Composable
fun TabletReschedulingCard(
    messageList: List<MessageCacheEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .sharedBoundsTransition("academic_messages")
            .height(TABLET_CARD_HEIGHT)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最新调课",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "最新2条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = dividerColor
            )

            // 内容区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messageList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无调课通知",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtextColor
                        )
                    }
                } else {
                    // 显示最新2条
                    messageList.take(2).forEach { message ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                        ) {
                            TabletMessageItem(message, textColor, subtextColor)
                        }
                    }

                    // 如果只有1条，添加占位
                    if (messageList.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 底部提示
            if (messageList.size > 2) {
                Text(
                    text = "还有 ${messageList.size - 2} 条通知",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * 平板端消息项
 */
@Composable
private fun TabletMessageItem(
    message: MessageCacheEntity,
    textColor: Color,
    subtextColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            lineHeight = 20.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        if (message.date > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(message.date),
                style = MaterialTheme.typography.labelSmall,
                color = subtextColor
            )
        }
    }
}

/**
 * 平板端近期考试卡片 - 固定高度，显示最近4条
 */
@Composable
fun TabletUpcomingExamsCard(
    examList: List<ExamUiState>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val secondaryColor = if (isDarkTheme) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .sharedBoundsTransition("exams")
            .height(TABLET_CARD_HEIGHT)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = secondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "近期考试",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                if (examList.isNotEmpty()) {
                    Text(
                        text = "共${examList.size}场",
                        style = MaterialTheme.typography.labelMedium,
                        color = subtextColor
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = dividerColor
            )

            // 内容区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (examList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无考试安排",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtextColor
                        )
                    }
                } else {
                    // 显示最近4条
                    examList.take(4).forEach { exam ->
                        TabletExamRowItem(exam, textColor, subtextColor)
                    }

                    // 如果不足4条，填充空间
                    if (examList.size < 4) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 底部提示
            if (examList.size > 4) {
                Text(
                    text = "还有 ${examList.size - 4} 场考试",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * 平板端考试行项目 - 更紧凑的布局
 */
@Composable
private fun TabletExamRowItem(
    exam: ExamUiState,
    textColor: Color,
    subtextColor: Color
) {
    val (countDownText, countColor) = remember(exam.time) {
        getExamCountDown(exam.time)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：时间块（月/日）
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(width = 44.dp, height = 44.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val timeStr = exam.time
                // 兼容两种格式: "2024-06-15(09:00-11:00)" 或 "2024-06-15 09:00-11:00"
                val datePart = timeStr.substringBefore("(").takeIf { it != timeStr }
                    ?: timeStr.split(" ").firstOrNull() ?: ""
                val parts = datePart.trim().split("-")
                if (parts.size >= 3) {
                    Text(
                        text = parts[1], // 月
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = parts[2], // 日
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 中间：课程名
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exam.courseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = exam.location,
                style = MaterialTheme.typography.labelSmall,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 右侧：倒计时
        Text(
            text = countDownText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = countColor
        )
    }
}

/**
 * 最新调课卡片 - 显示完整内容
 */
@Composable
fun ReschedulingCard(
    messageList: List<MessageCacheEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .sharedBoundsTransition("academic_messages")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最新调课",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (messageList.isNotEmpty()) {
                    Text(
                        text = "查看全部 >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            if (messageList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    messageList.take(2).forEach { message ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (message.date > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatTimestamp(message.date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (messageList.size > 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "还有 ${messageList.size - 2} 条通知",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                Text(
                    text = "暂无最新调课通知",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
        }
    }
}

/**
 * 调课信息全部对话框
 */
@Composable
fun MessagesDialog(
    messages: List<MessageCacheEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "调课通知",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                HorizontalDivider()

                // 消息列表
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无调课通知",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages) { message ->
                            MessageItem(message)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单条消息项
 */
@Composable
private fun MessageItem(message: MessageCacheEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
            if (message.date > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTimestamp(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.monthNumber}月${localDateTime.dayOfMonth}日 ${
            localDateTime.hour.toString().padStart(2, '0')
        }:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        ""
    }
}

/**
 * 近期考试卡片
 */
@Composable
fun UpcomingExamsCard(
    examList: List<ExamUiState>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .sharedBoundsTransition("exams")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "近期考试",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (examList.isNotEmpty()) {
                    Text(
                        text = "共${examList.size}场",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            if (examList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无考试安排", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    examList.take(4).forEach { exam ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            ExamRowItem(exam)
                        }
                    }
                    if (examList.size > 4) {
                        Text(
                            text = "查看更多...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 考试行项目
 */
@Composable
fun ExamRowItem(exam: ExamUiState) {
    val (countDownText, countColor) = remember(exam.time) {
        getExamCountDown(exam.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：时间块（月/日）
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(width = 50.dp, height = 50.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val timeStr = exam.time
                val datePart = timeStr.substringBefore("(")
                val parts = datePart.split("-")
                if (parts.size >= 3) {
                    Text(
                        text = parts[1], // 月
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = parts[2], // 日
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Text("待定", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧：详情
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (countDownText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = countColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = countDownText,
                            color = countColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val timeStr = exam.time
            val timePart = timeStr.substringAfter("(").substringBefore(")")
            val location = exam.location
            Text(
                text = "$timePart @ $location",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
