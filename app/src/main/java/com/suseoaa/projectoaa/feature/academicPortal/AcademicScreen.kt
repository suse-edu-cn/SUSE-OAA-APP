package com.suseoaa.projectoaa.feature.academicPortal

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.app.LocalWindowSizeClass
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec
import com.suseoaa.projectoaa.core.util.getExamCountDown
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.ExamUiState
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.GetExamInfoViewModel
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PortalFunction(
    val title: String,
    val icon: ImageVector,
    val destination: AcademicDestinations,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicScreen(
    isTablet: Boolean,
    onNavigate: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // 获取系统栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = if (!isTablet) 80.dp + navBarHeight else 16.dp
    val messageVM: GetAcademicMessageInfoViewModel = hiltViewModel()
    val examVM: GetExamInfoViewModel = hiltViewModel()

    // 使用 Flow 收集数据
    val messageList by messageVM.dataList.collectAsStateWithLifecycle()
    val examList by examVM.examList.collectAsStateWithLifecycle()

    val isRefreshing = examVM.isRefreshing || messageVM.isRefreshing
    val pullState = rememberPullToRefreshState()

    // 错峰加载策略
    LaunchedEffect(Unit) {
        delay(800)
        if (examList.isEmpty()) examVM.refreshData()
        if (messageList.isEmpty()) messageVM.refreshData()
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val isPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val gridColumns = if (isPhone) 2 else 4

    val functions = listOf(
        PortalFunction(
            "成绩查询",
            Icons.AutoMirrored.Filled.Assignment,
            AcademicDestinations.Grades,
            MaterialTheme.colorScheme.primary
        ),
//        PortalFunction(
//            "考试查询",
//            Icons.Default.Event,
//            AcademicDestinations.Exams,
//            MaterialTheme.colorScheme.tertiary
//        ),
        PortalFunction(
            "绩点计算",
            Icons.Default.Calculate, // 也可以用 Icons.AutoMirrored.Filled.ShowChart
            AcademicDestinations.Gpa,
            MaterialTheme.colorScheme.secondary
        )
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullState,
        onRefresh = {
            examVM.refreshData()
            messageVM.refreshData()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        with(sharedTransitionScope) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(
                    top = 16.dp + statusBarHeight,
                    bottom = 16.dp + bottomContentPadding,
                    start = 16.dp,
                    end = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. 调课信息
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "academic_messages_card"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = AcademicSharedTransitionSpec,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            zIndexInOverlay = 1f
                        )
                    ) {
                        ReschedulingCard(
                            messageList = messageList,
                            onClick = {
                                onNavigate(
                                    AcademicPortalEvent.NavigateTo(
                                        AcademicDestinations.Messages
                                    )
                                )
                            }
                        )
                    }
                }

                // 2. 考试信息
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "academic_exams_card"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = AcademicSharedTransitionSpec,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            zIndexInOverlay = 1f
                        )
                    ) {
                        UpcomingExamsCard(
                            examList = examList,
                            onClick = {
                                onNavigate(
                                    AcademicPortalEvent.NavigateTo(
                                        AcademicDestinations.Exams
                                    )
                                )
                            }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "常用功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(functions) { func ->
                    val cardKey = "${func.destination.route}_card"
                    if (func.destination == AcademicDestinations.Exams) {
                        FunctionCard(
                            func,
                            { onNavigate(AcademicPortalEvent.NavigateTo(func.destination)) })
                    } else {
                        Box(
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = cardKey),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = AcademicSharedTransitionSpec,
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                            )
                        ) {
                            FunctionCard(
                                func,
                                { onNavigate(AcademicPortalEvent.NavigateTo(func.destination)) })
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// 组件：最新调课卡片
@Composable
fun ReschedulingCard(
    messageList: List<String>?,
    onClick: () -> Unit
) {
    val latestMessage = messageList?.firstOrNull() ?: "暂无最新调课通知"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(
                    text = "查看全部 >",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = latestMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (messageList.isNullOrEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

// 组件：近期考试卡片 (关键修改处)
@Composable
fun UpcomingExamsCard(
    examList: List<ExamUiState>?,
    onClick: () -> Unit
) {
    // [修改] 移除 sortedBy，直接使用 ViewModel 传来的已排序列表
    val sortedExams = remember(examList) {
        examList ?: emptyList()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
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
                        imageVector = Icons.Default.Book,
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
                if (sortedExams.isNotEmpty()) {
                    Text(
                        text = "共${sortedExams.size}场",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            if (sortedExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无考试安排", color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 只展示前5个（因为已经按优先级排好了：未开始的在前，已结束的在最后）
                    sortedExams.take(5).forEach { exam ->
                        ExamRowItem(exam)
                    }
                    if (sortedExams.size > 5) {
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
                val datePart = exam.time.substringBefore("(")
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

            Text(
                text = "${exam.time.substringAfter("(").substringBefore(")")} @ ${exam.location}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 组件：功能按钮卡片
@Composable
fun FunctionCard(
    function: PortalFunction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = function.color.copy(alpha = 0.1f),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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