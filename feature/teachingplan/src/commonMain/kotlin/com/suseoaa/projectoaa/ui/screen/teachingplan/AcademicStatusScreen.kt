package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.presentation.teachingplan.AcademicStatusViewModel
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.common.ValueLabelStatItem
import com.suseoaa.projectoaa.ui.component.useTabletLayout
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import com.suseoaa.projectoaa.ui.screen.teachingplan.academicstatus.AcademicCategoryCard
import com.suseoaa.projectoaa.ui.screen.teachingplan.academicstatus.FilterChipRow

// 学业情况页：总览卡片与按类别分组的课程。

/**
 * 格式化浮点数为字符串（跨平台兼容）
 */
internal fun formatDouble(value: Double, decimals: Int = 1): String {
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

internal fun formatDouble(value: Float, decimals: Int = 1): String =
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

    AdaptivePageScaffold(
        title = "学业情况查询",
        onBack = onBack,
        sharedTransitionKey = "academicStatus",
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
        }
    ) { contentModifier ->
        // 错误提示
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                ToastManager.showToast(error)
                viewModel.clearError()
            }
        }

        AdaptiveLayout(
            modifier = contentModifier
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
