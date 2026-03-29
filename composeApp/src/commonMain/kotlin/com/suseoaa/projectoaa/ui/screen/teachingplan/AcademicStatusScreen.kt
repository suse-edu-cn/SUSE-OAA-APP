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
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.presentation.teachingplan.AcademicStatusViewModel
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.common.ValueLabelStatItem
import com.suseoaa.projectoaa.ui.component.useTabletLayout
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
        modifier = Modifier.sharedBoundsTransition("academicStatus"),
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

        AdaptiveLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) { adaptiveLayoutConfig ->
            val isTablet = adaptiveLayoutConfig.useTabletLayout()
            val expandedCategoryIds = uiState.expandedCategories
            val selectedFilter = uiState.selectedFilter
            val filteredCoursesByCategory by remember(uiState.categories, selectedFilter) {
                derivedStateOf {
                    uiState.categories.associate { category ->
                        category.categoryId to viewModel.getFilteredCourses(category.courses)
                    }
                }
            }

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
                        // 毕业进度总览卡片
                        if (uiState.planOverview.totalRequiredCredits > 0) {
                            item {
                                PlanOverviewCard(
                                    planOverview = uiState.planOverview,
                                    averageGradePoint = uiState.averageGradePoint,
                                    studyingCredits = uiState.studyingCredits,
                                    planTotalCourses = uiState.planTotalCourses,
                                    planPassedCount = uiState.planPassedCount,
                                    planFailedCount = uiState.planFailedCount,
                                    planStudyingCount = uiState.planStudyingCount,
                                    planNotStudiedCount = uiState.planNotStudiedCount,
                                    nonPlanPassedCount = uiState.nonPlanPassedCount,
                                    nonPlanFailedCount = uiState.nonPlanFailedCount,
                                    isTablet = isTablet
                                )
                            }
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
                            key = { it.categoryId },
                            contentType = { "academic_category_card" }
                        ) { category ->
                            AcademicCategoryCard(
                                category = category,
                                isExpanded = expandedCategoryIds.contains(category.categoryId),
                                onToggleExpand = { viewModel.toggleCategoryExpanded(category.categoryId) },
                                filteredCourses = filteredCoursesByCategory[category.categoryId].orEmpty(),
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
 * 毕业进度总览卡片
 * 显示教学计划名称、总学分要求、已获学分、未获学分、平均绩点
 */
@Composable
private fun PlanOverviewCard(
    planOverview: AcademicPlanOverview,
    averageGradePoint: Double,
    studyingCredits: Double,
    planTotalCourses: Int,
    planPassedCount: Int,
    planFailedCount: Int,
    planStudyingCount: Int,
    planNotStudiedCount: Int,
    nonPlanPassedCount: Int,
    nonPlanFailedCount: Int,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (planOverview.totalRequiredCredits > 0) {
        (planOverview.totalEarnedCredits / planOverview.totalRequiredCredits).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "plan_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 计划名称和通过状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planOverview.planName.ifEmpty { "教学计划" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (planOverview.isPassed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "已达标",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "进行中",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 总体进度条
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "毕业学分进度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatDouble(planOverview.totalEarnedCredits)} / ${formatDouble(planOverview.totalRequiredCredits)} 学分",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = if (planOverview.isPassed) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            // 统计信息行
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ValueLabelStatItem(
                        label = "要求学分",
                        value = formatDouble(planOverview.totalRequiredCredits, 1),
                        color = MaterialTheme.colorScheme.primary,
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "已获学分",
                        value = formatDouble(planOverview.totalEarnedCredits, 1),
                        color = Color(0xFF4CAF50),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "未获学分",
                        value = formatDouble(planOverview.totalRemainingCredits, 1),
                        color = Color(0xFFFF9800),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "在修学分",
                        value = formatDouble(studyingCredits, 1),
                        color = Color(0xFF2196F3),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "平均绩点",
                        value = formatDouble(averageGradePoint, 2),
                        color = MaterialTheme.colorScheme.error,
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ValueLabelStatItem(
                        label = "已获学分",
                        value = formatDouble(planOverview.totalEarnedCredits, 1),
                        color = Color(0xFF4CAF50),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "未获学分",
                        value = formatDouble(planOverview.totalRemainingCredits, 1),
                        color = Color(0xFFFF9800),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "在修学分",
                        value = formatDouble(studyingCredits, 1),
                        color = Color(0xFF2196F3),
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                    ValueLabelStatItem(
                        label = "平均绩点",
                        value = formatDouble(averageGradePoint, 2),
                        color = MaterialTheme.colorScheme.error,
                        valueTextStyle = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // 课程统计摘要
            if (planTotalCourses > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append("计划总课程 $planTotalCourses 门")
                            append("  通过 $planPassedCount 门")
                            if (planFailedCount > 0) append("，未通过 $planFailedCount 门\n")
                            append("未修 $planNotStudiedCount 门\n")
                            append("在读 $planStudyingCount 门")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (nonPlanPassedCount > 0 || nonPlanFailedCount > 0) {
                        Text(
                            text = buildString {
                                append("计划外：")
                                append("通过 $nonPlanPassedCount 门")
                                if (nonPlanFailedCount > 0) append("，未通过 $nonPlanFailedCount 门")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
 * 课程类别卡片 - 含学分要求进度条
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
