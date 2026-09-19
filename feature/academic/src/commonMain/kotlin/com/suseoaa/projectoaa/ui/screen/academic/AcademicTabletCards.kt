package com.suseoaa.projectoaa.ui.screen.academic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.shared.domain.model.message.MessageCacheEntity
import com.suseoaa.projectoaa.presentation.academic.ExamUiState
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.getExamCountDown
import kotlinx.datetime.*

// 平板宽屏下的调课与临考卡片。

// 平板卡片固定高度常量（基于4条考试信息的高度）
internal val TABLET_CARD_HEIGHT = 340.dp

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
internal fun TabletMessageItem(
    message: MessageCacheEntity,
    textColor: Color,
    subtextColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        val displayContent = message.aiSummary ?: message.content
        Text(
            text = displayContent,
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
internal fun TabletExamRowItem(
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
