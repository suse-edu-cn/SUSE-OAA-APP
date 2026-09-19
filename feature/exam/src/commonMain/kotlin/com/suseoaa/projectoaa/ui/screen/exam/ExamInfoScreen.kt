package com.suseoaa.projectoaa.ui.screen.exam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.domain.exam.ExamUiItem
import com.suseoaa.projectoaa.presentation.exam.ExamUiState
import com.suseoaa.projectoaa.presentation.exam.ExamViewModel
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.getExamCountDown
import org.koin.compose.viewmodel.koinViewModel

// 考试信息页：手机/平板两套布局与考试卡片。

/**
 * 考试信息查询界面
 * 支持查询不同学期的考试信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamInfoScreen(
    onBack: () -> Unit,
    viewModel: ExamViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()

    // 颜色定义
    val backgroundColor = if (isDarkTheme) NightBackground else OxygenBackground
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack

    // 错误提示
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            ToastManager.showToast(error)
            viewModel.clearError()
        }
    }

    // 编辑对话框
    val editingExam = uiState.editingExam
    if (uiState.showEditDialog && editingExam != null) {
        ExamEditDialog(
            exam = editingExam,
            isAddMode = uiState.isAddMode,
            isDarkTheme = isDarkTheme,
            onSave = { viewModel.saveExam(it) },
            onDelete = { viewModel.deleteExam(it) },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    AdaptivePageScaffold(
        sharedTransitionKey = "exams",
        title = "考试信息查询",
        onBack = onBack,
        containerColor = backgroundColor,
        topBarContainerColor = surfaceColor,
        titleColor = textColor,
        navigationIconColor = textColor,
        compactPadding = 0.dp,
        tabletPadding = 0.dp,
        actions = {
            IconButton(onClick = { viewModel.showAddExamDialog() }) {
                Icon(
                    Icons.Default.Add,
                    "添加考试",
                    tint = primaryColor
                )
            }

            IconButton(
                onClick = { viewModel.refresh() },
                enabled = !uiState.isRefreshing
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = primaryColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        "刷新",
                        tint = primaryColor
                    )
                }
            }
        },
        compactContent = { modifier ->
            PhoneExamLayout(
                uiState = uiState,
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                modifier = modifier
            )
        },
        tabletContent = { modifier ->
            TabletExamLayout(
                uiState = uiState,
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                modifier = modifier
            )
        }
    )
}

// ============================================================================
// 平板布局
// ============================================================================

@Composable
private fun TabletExamLayout(
    uiState: ExamUiState,
    viewModel: ExamViewModel,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else OutlineSoft

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.screenPaddingMedium, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.paneSpacing)
    ) {
        // 左侧筛选面板
        Card(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = "学期选择",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )

                HorizontalDivider(color = dividerColor)

                // 学期列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableSemesters) { semester ->
                        val isSelected = semester.year == uiState.selectedYear &&
                                semester.semester == uiState.selectedSemester

                        SemesterOptionItem(
                            option = semester,
                            isSelected = isSelected,
                            onClick = { viewModel.selectSemester(semester) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }

        // 右侧内容区域
        Column(modifier = Modifier.weight(1f)) {
            // 统计信息
            if (uiState.exams.isNotEmpty()) {
                ExamStatisticsBar(
                    totalCount = uiState.exams.size,
                    upcomingCount = uiState.exams.count { !it.isEnded },
                    endedCount = uiState.exams.count { it.isEnded },
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 考试列表
            ExamContentArea(
                uiState = uiState,
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                columns = 2
            )
        }
    }
}

// ============================================================================
// 手机布局
// ============================================================================

@Composable
private fun PhoneExamLayout(
    uiState: ExamUiState,
    viewModel: ExamViewModel,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 可折叠学期选择器
        CollapsibleSemesterSelector(
            isExpanded = uiState.isFilterExpanded,
            onToggle = { viewModel.toggleFilterExpanded() },
            selectedDisplay = viewModel.getSelectedSemesterDisplay(),
            semesters = uiState.availableSemesters,
            selectedYear = uiState.selectedYear,
            selectedSemester = uiState.selectedSemester,
            onSelect = { viewModel.selectSemester(it) },
            isDarkTheme = isDarkTheme
        )

        // 统计信息
        if (uiState.exams.isNotEmpty()) {
            ExamStatisticsBar(
                totalCount = uiState.exams.size,
                upcomingCount = uiState.exams.count { !it.isEnded },
                endedCount = uiState.exams.count { it.isEnded },
                isDarkTheme = isDarkTheme
            )
        }

        // 考试列表
        ExamContentArea(
            uiState = uiState,
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            columns = 1
        )
    }
}

// ============================================================================
// 共享组件
// ============================================================================

/**
 * 内容区域
 */
@Composable
private fun ExamContentArea(
    uiState: ExamUiState,
    viewModel: ExamViewModel,
    isDarkTheme: Boolean,
    columns: Int
) {
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "正在加载考试信息...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subtextColor
                    )
                }
            }
        }

        uiState.exams.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = subtextColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "该学期暂无考试信息",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subtextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("点击刷新", color = primaryColor)
                    }
                }
            }
        }

        else -> {
            val navBarHeight =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp + navBarHeight
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.exams) { exam ->
                    ExamCard(
                        exam = exam,
                        isDarkTheme = isDarkTheme,
                        onEdit = { viewModel.showEditExamDialog(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 考试卡片
 */
@Composable
internal fun ExamCard(
    exam: ExamUiItem,
    isDarkTheme: Boolean,
    onEdit: (ExamUiItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else OutlineSoft
    val customBadgeColor = Color(0xFF34C759) // iOS 绿色

    // 倒计时
    val (countDownText, countColor) = remember(exam.time) {
        getExamCountDown(exam.time)
    }

    // 已结束的考试使用淡化效果
    val cardAlpha = if (exam.isEnded) 0.6f else 1f

    Card(
        modifier = modifier.then(
            if (exam.isCustom) {
                Modifier.clickable { onEdit(exam) }
            } else {
                Modifier
            }
        ),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 头部：课程名 + 自定义标签 + 倒计时
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exam.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (exam.isEnded) subtextColor else primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 自定义考试标签
                    if (exam.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = customBadgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "自定义",
                                color = customBadgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (countDownText.isNotEmpty()) {
                        Surface(
                            color = countColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = countDownText,
                                color = countColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 自定义考试显示编辑图标
                    if (exam.isCustom) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(18.dp),
                            tint = subtextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 考试时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = subtextColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = exam.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = cardAlpha)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 考试地点
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = subtextColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = exam.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = cardAlpha)
                )
            }

            // 如果有学分信息，显示分割线和额外信息
            if (exam.credit.isNotEmpty() || exam.examName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (exam.credit.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = subtextColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${exam.credit}学分",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtextColor
                            )
                        }
                    }

                    Text(
                        text = exam.examType,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtextColor
                    )
                }
            }
        }
    }
}
