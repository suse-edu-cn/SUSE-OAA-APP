package com.suseoaa.projectoaa.ui.screen.academic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.academic.AcademicViewModel
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamItem
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

data class PortalFunction(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@Composable
fun AcademicScreen(
    onNavigateToGrades: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToExams: () -> Unit,
    viewModel: AcademicViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = 80.dp + navBarHeight

    val functions = listOf(
        PortalFunction(
            "成绩查询",
            Icons.AutoMirrored.Filled.List,
            "grades",
            ElectricBlue
        ),
        PortalFunction(
            "绩点计算",
            Icons.Default.Star,
            "gpa",
            Color(0xFF26A69A)
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
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
        // 1. 调课信息卡片
        item(span = { GridItemSpan(maxLineSpan) }) {
            ReschedulingCard(
                messageList = uiState.messages,
                onClick = { /* TODO: 导航到消息详情 */ }
            )
        }

        // 2. 近期考试卡片
        item(span = { GridItemSpan(maxLineSpan) }) {
            UpcomingExamsCard(
                examList = uiState.exams,
                onClick = onNavigateToExams
            )
        }

        // 3. 常用功能标题
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "常用功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // 4. 功能卡片
        items(functions) { func ->
            FunctionCard(
                function = func,
                onClick = {
                    when (func.route) {
                        "grades" -> onNavigateToGrades()
                        "gpa" -> onNavigateToGpa()
                    }
                }
            )
        }

        // 底部空间
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * 最新调课卡片
 */
@Composable
fun ReschedulingCard(
    messageList: List<String>,
    onClick: () -> Unit
) {
    val latestMessage = messageList.firstOrNull() ?: "暂无最新调课通知"

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
                color = if (messageList.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

/**
 * 近期考试卡片
 */
@Composable
fun UpcomingExamsCard(
    examList: List<ExamItem>,
    onClick: () -> Unit
) {
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
                    Text("暂无考试安排", color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    examList.take(5).forEach { exam ->
                        ExamRowItem(exam)
                    }
                    if (examList.size > 5) {
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
fun ExamRowItem(exam: ExamItem) {
    val (countDownText, countColor) = remember(exam.kssj) {
        getExamCountDown(exam.kssj ?: "")
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
                val timeStr = exam.kssj ?: ""
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
                    text = exam.kcmc ?: "未知考试",
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

            val timeStr = exam.kssj ?: ""
            val timePart = timeStr.substringAfter("(").substringBefore(")")
            val location = exam.cdmc ?: "待定"
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

/**
 * 计算考试倒计时
 */
fun getExamCountDown(examTime: String): Pair<String, Color> {
    if (examTime.isBlank()) return "" to Color.Gray

    try {
        // 解析日期部分 "2024-06-15(周六 09:00-11:00)"
        val datePart = examTime.substringBefore("(")
        val parts = datePart.split("-")
        if (parts.size < 3) return "" to Color.Gray

        val year = parts[0].toIntOrNull() ?: return "" to Color.Gray
        val month = parts[1].toIntOrNull() ?: return "" to Color.Gray
        val day = parts[2].toIntOrNull() ?: return "" to Color.Gray

        val examDate = LocalDate(year, month, day)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysUntil: Int = examDate.toEpochDays() - today.toEpochDays()

        return when {
            daysUntil < 0 -> "已结束" to Color.Gray
            daysUntil == 0 -> "今天" to Color(0xFFC62828)
            daysUntil == 1 -> "明天" to Color(0xFFEF6C00)
            daysUntil <= 3 -> "${daysUntil}天后" to Color(0xFFEF6C00)
            daysUntil <= 7 -> "${daysUntil}天后" to Color(0xFF1565C0)
            else -> "${daysUntil}天后" to Color(0xFF2E7D32)
        }
    } catch (e: Exception) {
        return "" to Color.Gray
    }
}
