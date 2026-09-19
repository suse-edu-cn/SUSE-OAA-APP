package com.suseoaa.projectoaa.ui.screen.teachingplan.academicstatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.presentation.teachingplan.AcademicStatusFilter
import com.suseoaa.projectoaa.ui.screen.teachingplan.formatDouble

// 课程类别卡片与课程表格行。

/**
 * 筛选器芯片行
 */
@Composable
internal fun FilterChipRow(
    selectedFilter: AcademicStatusFilter,
    onFilterSelect: (AcademicStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AcademicStatusFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelect(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = if (selectedFilter == filter) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}

/**
 * 课程类别卡片 - 含学分要求进度条
 */
@Composable
internal fun AcademicCategoryCard(
    category: AcademicStatusCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    filteredCourses: List<AcademicStatusCourseItem>,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    // 计算学分完成进度
    val creditProgress = if (category.requiredCredits > 0) {
        (category.systemEarnedCredits / category.requiredCredits).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedCreditProgress by animateFloatAsState(
        targetValue = creditProgress,
        label = "credit_progress"
    )

    val remainingCredits = (category.requiredCredits - category.systemEarnedCredits).coerceAtLeast(0.0)
    val categoryColor = getCategoryColor(category.categoryName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 类别头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 类别图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.categoryName),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // 通过/未通过标签
                            if (category.requiredCredits > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (category.isPassed)
                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    else
                                        Color(0xFFFF9800).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (category.isPassed) "已达标" else "未达标",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (category.isPassed) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 学分要求进度
                        if (category.requiredCredits > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "要求 ${formatDouble(category.requiredCredits)} 学分",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (remainingCredits > 0) {
                                    Text(
                                        text = "还差 ${formatDouble(remainingCredits)} 学分",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                } else {
                                    Text(
                                        text = "已获 ${formatDouble(category.systemEarnedCredits)} 学分",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { animatedCreditProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = if (category.isPassed) Color(0xFF4CAF50) else categoryColor
                            )
                        } else if (category.isLoaded) {
                            Text(
                                text = "${category.passedCount}门已过 · ${category.studyingCount}门在修 · " +
                                        "${formatDouble(category.earnedCredits, 1)}/${formatDouble(category.totalCredits, 1)}学分",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 加载指示器或展开箭头
                if (category.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier.rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 展开的课程列表
            AnimatedVisibility(
                visible = isExpanded && category.isLoaded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    if (filteredCourses.isEmpty()) {
                        Text(
                            text = "无匹配课程",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        if (isTablet) {
                            // 平板：表格样式
                            CourseTableHeader()
                            filteredCourses.forEach { course ->
                                CourseTableRow(course = course)
                            }
                        } else {
                            // 手机：卡片样式
                            filteredCourses.forEach { course ->
                                CourseItemCard(course = course)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课程表格头部（平板）
 */
@Composable
internal fun CourseTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "课程名称",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = "学分",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "成绩",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "绩点",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "状态",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 课程表格行（平板）
 */
@Composable
internal fun CourseTableRow(
    course: AcademicStatusCourseItem,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(course.studyStatus)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (course.yearName.isNotEmpty()) {
                Text(
                    text = "${course.yearName} 第${course.semesterName}学期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = course.credits,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = course.grade.ifEmpty { "-" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (course.studyStatus == StudyStatusUtils.PASSED) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = if (course.gradePoint > 0) formatDouble(course.gradePoint, 1) else "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        StatusBadge(
            status = course.studyStatus,
            modifier = Modifier.width(70.dp)
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/**
 * 课程卡片（手机）
 */
@Composable
internal fun CourseItemCard(
    course: AcademicStatusCourseItem,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(course.studyStatus)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${course.credits}学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (course.grade.isNotEmpty()) {
                        Text(
                            text = "成绩: ${course.grade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (course.gradePoint > 0) {
                        Text(
                            text = "绩点: ${formatDouble(course.gradePoint, 1)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (course.yearName.isNotEmpty()) {
                    Text(
                        text = "${course.yearName} 第${course.semesterName}学期",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            StatusBadge(status = course.studyStatus)
        }
    }
}

/**
 * 状态徽章
 */
@Composable
internal fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(status)
    val statusName = StudyStatusUtils.getStatusName(status)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = statusColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = statusName,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
