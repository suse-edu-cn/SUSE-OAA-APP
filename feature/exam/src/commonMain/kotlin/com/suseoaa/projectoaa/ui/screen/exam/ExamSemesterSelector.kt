package com.suseoaa.projectoaa.ui.screen.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.domain.exam.SemesterOption
import com.suseoaa.projectoaa.ui.component.common.ValueLabelStatItem
import com.suseoaa.projectoaa.ui.theme.*

// 学期选择器与顶部统计条。

/**
 * 可折叠学期选择器（手机端）
 * 当前选中的学期始终可见，点击展开可选择其他学期
 */
@Composable
internal fun CollapsibleSemesterSelector(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedDisplay: String,
    semesters: List<SemesterOption>,
    selectedYear: String,
    selectedSemester: String,
    onSelect: (SemesterOption) -> Unit,
    isDarkTheme: Boolean
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val containerColor = if (isDarkTheme) NightContainer else SoftBlueWait
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else OutlineSoft

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 当前选中的学期（始终可见）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 当前选中的学期标签
                    Surface(
                        color = primaryColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = selectedDisplay,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // 展开/收起按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "收起" else "切换学期",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 可折叠的学期选项列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(semesters) { semester ->
                        val isSelected = semester.year == selectedYear &&
                                semester.semester == selectedSemester

                        SemesterChip(
                            option = semester,
                            isSelected = isSelected,
                            onClick = { onSelect(semester) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor)
        }
    }
}

/**
 * 学期选项项（平板端列表）
 */
@Composable
internal fun SemesterOptionItem(
    option: SemesterOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDarkTheme) NightContainer else SoftBlueWait
        } else {
            Color.Transparent
        },
        label = "bg"
    )
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) primaryColor else textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 学期选项 Chip（手机端横向滚动）
 */
@Composable
internal fun SemesterChip(
    option: SemesterOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor = if (isSelected) {
        if (isDarkTheme) NightBlue else ElectricBlue
    } else {
        if (isDarkTheme) NightContainer else SoftBlueWait
    }
    val textColor = if (isSelected) {
        Color.White
    } else {
        if (isDarkTheme) Color.White else InkBlack
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * 统计栏
 */
@Composable
internal fun ExamStatisticsBar(
    totalCount: Int,
    upcomingCount: Int,
    endedCount: Int,
    isDarkTheme: Boolean
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val warningColor = Color(0xFFFF9500) // iOS 橙色
    val successColor = Color(0xFF34C759) // iOS 绿色

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ValueLabelStatItem(
                label = "共计",
                value = "$totalCount",
                color = primaryColor
            )
            ValueLabelStatItem(
                label = "待考",
                value = "$upcomingCount",
                color = warningColor
            )
            ValueLabelStatItem(
                label = "已结束",
                value = "$endedCount",
                color = successColor
            )
        }
    }
}
