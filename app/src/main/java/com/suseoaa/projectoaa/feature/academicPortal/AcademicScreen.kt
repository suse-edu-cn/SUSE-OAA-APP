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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
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
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.ExamUiState
import com.suseoaa.projectoaa.feature.academicPortal.getExamInfo.GetExamInfoViewModel
import com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo.GetAcademicMessageInfoViewModel

// 定义功能按钮的数据结构
data class PortalFunction(
    val title: String,
    val icon: ImageVector,
    val destination: AcademicDestinations,
    val color: Color
)

@Composable
fun AcademicScreen(
    isTablet: Boolean,
    onNavigate: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // 1. 获取 ViewModel
    val messageVM: GetAcademicMessageInfoViewModel = hiltViewModel()
    val examVM: GetExamInfoViewModel = hiltViewModel()

    // 2. 收集数据状态
    val messageList by messageVM.dataList.collectAsStateWithLifecycle()
    val examList by examVM.dataList.collectAsStateWithLifecycle()

    // 3. 触发数据加载
    LaunchedEffect(Unit) {
        messageVM.fetchData()
        examVM.fetchData()
    }

    // 4. 获取窗口大小以决定列数
    val windowSizeClass = LocalWindowSizeClass.current
    val isPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    // 手机一行2个，平板一行4个
    val gridColumns = if (isPhone) 2 else 4

    // 5. 定义功能按钮列表
    val functions = listOf(
        PortalFunction(
            "成绩查询",
            Icons.Default.Assignment,
            AcademicDestinations.Grades,
            MaterialTheme.colorScheme.primary
        ),
        PortalFunction(
            "考场查询",
            Icons.Default.Event,
            AcademicDestinations.Exams,
            MaterialTheme.colorScheme.tertiary
        ),
        // 可以在这里添加更多功能，布局会自动适配
        // PortalFunction("空教室", Icons.Default.MeetingRoom, ...),
    )

    with(sharedTransitionScope) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // === 第一部分：调课信息 (占满整行) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                // 外层Box用于承载共享元素转场动画
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
                        // 点击跳转到所有消息列表页
                        onClick = { onNavigate(AcademicPortalEvent.NavigateTo(AcademicDestinations.Messages)) }
                    )
                }
            }

            // === 第二部分：考试信息 (占满整行) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "exam_card_key"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = AcademicSharedTransitionSpec,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                    )
                ) {
                    UpcomingExamsCard(
                        examList = examList,
                        onClick = { onNavigate(AcademicPortalEvent.NavigateTo(AcademicDestinations.Exams)) }
                    )
                }
            }

            // === 第三部分：功能栏标题 ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "常用功能",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // === 第四部分：功能按钮网格 ===
            items(functions) { func ->
                Box(
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${func.destination.route}_card"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = AcademicSharedTransitionSpec,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                    )
                ) {
                    FunctionCard(
                        function = func,
                        onClick = { onNavigate(AcademicPortalEvent.NavigateTo(func.destination)) }
                    )
                }
            }

            // 底部留白，防止被 BottomBar 遮挡
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- 组件：最新调课卡片 ---
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
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- 组件：近期考试卡片 ---
@Composable
fun UpcomingExamsCard(
    examList: List<ExamUiState>?,
    onClick: () -> Unit
) {
    // 排序逻辑：按时间字符串排序 (yyyy-MM-dd 格式字符串排序等同于时间排序)
    val sortedExams = remember(examList) {
        examList?.sortedBy { it.time } ?: emptyList()
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
                // 显示列表，最多显示前 5 条以保证美观
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：时间块
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(width = 50.dp, height = 50.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 简单提取日期中的 "月-日"
                // 假设格式为 "2026-01-08(09:30-11:30)"
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
            Text(
                text = exam.courseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

// --- 组件：功能按钮卡片 ---
@Composable
fun FunctionCard(
    function: PortalFunction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = function.color.copy(alpha = 0.1f), // 浅色背景
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形图标背景
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