package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.presentation.teachingplan.AcademicStatusViewModel
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * 格式化浮点数为字符串（跨平台兼容）
 */
private fun formatDouble(value: Double, decimals: Int = 1): String {
    val multiplier = when (decimals) {
        1 -> 10.0
        2 -> 100.0
        else -> 10.0
    }
    val rounded = (value * multiplier).roundToInt() / multiplier
    return when (decimals) {
        1 -> {
            val intPart = rounded.toInt()
            val decPart = ((rounded - intPart) * 10).roundToInt()
            "$intPart.$decPart"
        }

        2 -> {
            val intPart = rounded.toInt()
            val decPart = ((rounded - intPart) * 100).roundToInt()
            "$intPart.${decPart.toString().padStart(2, '0')}"
        }

        else -> rounded.toString()
    }
}

private fun formatDouble(value: Float, decimals: Int = 1): String =
    formatDouble(value.toDouble(), decimals)

/**
 * 学业情况查询界面
 * 显示学生各类别课程的修读状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicStatusScreen(
    onBack: () -> Unit,
    viewModel: AcademicStatusViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学业情况查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 展开/折叠全部按钮
                    if (uiState.categories.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (uiState.expandedCategories.size == uiState.categories.size) {
                                    viewModel.collapseAllCategories()
                                } else {
                                    viewModel.expandAllCategories()
                                }
                            }
                        ) {
                            Icon(
                                if (uiState.expandedCategories.size == uiState.categories.size)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = if (uiState.expandedCategories.size == uiState.categories.size)
                                    "全部折叠" else "全部展开"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        // 错误提示
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                ToastManager.showToast(error)
                viewModel.clearError()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTablet = maxWidth > 600.dp

            if (uiState.isLoading && uiState.categories.isEmpty()) {
                // 初始加载
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = if (isTablet) 24.dp else 16.dp,
                            end = if (isTablet) 24.dp else 16.dp,
                            top = 16.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 统计卡片
                        item {
                            AcademicStatusStatsCard(
                                totalCredits = uiState.totalCredits,
                                earnedCredits = uiState.earnedCredits,
                                studyingCredits = uiState.studyingCredits,
                                averageGradePoint = uiState.averageGradePoint,
                                isTablet = isTablet
                            )
                        }

                        // 筛选器
                        item {
                            FilterChipRow(
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelect = viewModel::setFilter
                            )
                        }

                        // 课程类别列表
                        items(
                            items = uiState.categories,
                            key = { it.categoryId }
                        ) { category ->
                            AcademicCategoryCard(
                                category = category,
                                isExpanded = viewModel.isCategoryExpanded(category.categoryId),
                                onToggleExpand = { viewModel.toggleCategoryExpanded(category.categoryId) },
                                filteredCourses = viewModel.getFilteredCourses(category.courses),
                                isTablet = isTablet
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun AcademicStatusStatsCard(
    totalCredits: Double,
    earnedCredits: Double,
    studyingCredits: Double,
    averageGradePoint: Double,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        if (isTablet) {
            // 平板横向布局
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "应修学分",
                    value = formatDouble(totalCredits, 1),
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "已获学分",
                    value = formatDouble(earnedCredits, 1),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    label = "在修学分",
                    value = formatDouble(studyingCredits, 1),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    label = "平均绩点",
                    value = formatDouble(averageGradePoint, 2),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // 手机两行布局
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "应修学分",
                        value = formatDouble(totalCredits, 1),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        label = "已获学分",
                        value = formatDouble(earnedCredits, 1),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "在修学分",
                        value = formatDouble(studyingCredits, 1),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatItem(
                        label = "平均绩点",
                        value = formatDouble(averageGradePoint, 2),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 筛选器芯片行
 */
@Composable
private fun FilterChipRow(
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
 * 课程类别卡片
 */
@Composable
private fun AcademicCategoryCard(
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
                            .background(getCategoryColor(category.categoryName).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.categoryName),
                            contentDescription = null,
                            tint = getCategoryColor(category.categoryName),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (category.isLoaded) {
                            Text(
                                text = "${category.passedCount}门已过 · ${category.studyingCount}门在修 · " +
                                        "${formatDouble(category.earnedCredits, 1)}/${
                                            formatDouble(
                                                category.totalCredits,
                                                1
                                            )
                                        }学分",
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
private fun CourseTableHeader(modifier: Modifier = Modifier) {
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
private fun CourseTableRow(
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
private fun CourseItemCard(
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
private fun StatusBadge(
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

/**
 * 获取状态颜色
 */
@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        StudyStatusUtils.PASSED -> Color(0xFF4CAF50)      // 绿色
        StudyStatusUtils.FAILED -> Color(0xFFE53935)      // 红色
        StudyStatusUtils.STUDYING -> Color(0xFF2196F3)    // 蓝色
        StudyStatusUtils.NOT_STUDIED -> Color(0xFF9E9E9E) // 灰色
        else -> MaterialTheme.colorScheme.onSurface
    }
}

/**
 * 获取类别颜色
 */
@Composable
private fun getCategoryColor(categoryName: String): Color {
    return when {
        categoryName.contains("必修") -> MaterialTheme.colorScheme.primary
        categoryName.contains("选修") -> MaterialTheme.colorScheme.tertiary
        categoryName.contains("实践") -> MaterialTheme.colorScheme.secondary
        categoryName.contains("通识") -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * 获取类别图标
 */
private fun getCategoryIcon(categoryName: String) = when {
    categoryName.contains("必修") -> Icons.Default.Star
    categoryName.contains("选修") -> Icons.Default.Menu
    categoryName.contains("实践") -> Icons.Default.Build
    categoryName.contains("通识") -> Icons.Default.Info
    categoryName.contains("核心") -> Icons.Default.Star
    else -> Icons.Default.CheckCircle
}
