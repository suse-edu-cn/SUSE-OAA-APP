package com.suseoaa.projectoaa.ui.screen.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.presentation.exam.ExamUiItem
import com.suseoaa.projectoaa.presentation.exam.ExamUiState
import com.suseoaa.projectoaa.presentation.exam.ExamViewModel
import com.suseoaa.projectoaa.presentation.exam.SemesterOption
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.getExamCountDown
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

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
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("考试信息查询", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    // 添加考试按钮
                    IconButton(onClick = { viewModel.showAddExamDialog() }) {
                        Icon(
                            Icons.Default.Add,
                            "添加考试",
                            tint = primaryColor
                        )
                    }

                    // 刷新按钮
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth > 600.dp

            if (isTablet) {
                // 平板布局：左侧筛选面板，右侧内容
                TabletExamLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            } else {
                // 手机布局：可折叠筛选区域
                PhoneExamLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

// ============================================================================
// 平板布局
// ============================================================================

@Composable
private fun TabletExamLayout(
    uiState: ExamUiState,
    viewModel: ExamViewModel,
    isDarkTheme: Boolean
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else OutlineSoft

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    isDarkTheme: Boolean
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val containerColor = if (isDarkTheme) NightContainer else SoftBlueWait
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else OutlineSoft

    Column(modifier = Modifier.fillMaxSize()) {
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
 * 可折叠学期选择器（手机端）
 * 当前选中的学期始终可见，点击展开可选择其他学期
 */
@Composable
private fun CollapsibleSemesterSelector(
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
private fun SemesterOptionItem(
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
private fun SemesterChip(
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
private fun ExamStatisticsBar(
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
            StatItem(
                label = "共计",
                value = "$totalCount",
                color = primaryColor
            )
            StatItem(
                label = "待考",
                value = "$upcomingCount",
                color = warningColor
            )
            StatItem(
                label = "已结束",
                value = "$endedCount",
                color = successColor
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

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
private fun ExamCard(
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

/**
 * 考试编辑对话框
 */
@Composable
private fun ExamEditDialog(
    exam: ExamUiItem,
    isAddMode: Boolean,
    isDarkTheme: Boolean,
    onSave: (ExamUiItem) -> Unit,
    onDelete: (ExamUiItem) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val errorColor = AlertRed // iOS 风格红

    // 编辑状态
    var courseName by remember { mutableStateOf(exam.courseName) }
    var location by remember { mutableStateOf(exam.location) }
    var credit by remember { mutableStateOf(exam.credit) }
    var examType by remember { mutableStateOf(exam.examType) }

    // 日期时间状态
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var selectedDate by remember {
        mutableStateOf(parseExamDate(exam.time) ?: now.date)
    }
    var startHour by remember { mutableStateOf(parseExamStartHour(exam.time) ?: 9) }
    var startMinute by remember { mutableStateOf(parseExamStartMinute(exam.time) ?: 0) }
    var endHour by remember { mutableStateOf(parseExamEndHour(exam.time) ?: 11) }
    var endMinute by remember { mutableStateOf(parseExamEndMinute(exam.time) ?: 0) }

    // 显示日期选择器
    var showDatePicker by remember { mutableStateOf(false) }

    // 显示删除确认
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 格式化时间字符串 - 使用与API一致的格式: "2024-06-15(09:00-11:00)"
    fun formatTime(): String {
        val dateStr = "${selectedDate.year}-${
            selectedDate.monthNumber.toString().padStart(2, '0')
        }-${selectedDate.dayOfMonth.toString().padStart(2, '0')}"
        val startStr =
            "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}"
        val endStr =
            "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
        return "$dateStr($startStr-$endStr)"
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = surfaceColor,
            title = { Text("删除确认", color = textColor) },
            text = { Text("确定要删除这条考试信息吗？此操作无法撤销。", color = subtextColor) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(exam)
                    }
                ) {
                    Text("删除", color = errorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = subtextColor)
                }
            }
        )
    }

    // 日期选择器对话框
    if (showDatePicker) {
        ExamDatePicker(
            currentDate = selectedDate,
            isDarkTheme = isDarkTheme,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                text = if (isAddMode) "添加考试" else "编辑考试",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 课程名称（必填）
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedLabelColor = subtextColor,
                        cursorColor = primaryColor
                    )
                )

                // 考试日期（点击选择）
                Text(
                    text = "考试日期 *",
                    style = MaterialTheme.typography.labelMedium,
                    color = subtextColor
                )
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(
                        alpha = 0.05f
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedDate.year}年${selectedDate.monthNumber}月${selectedDate.dayOfMonth}日",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = subtextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 考试时间选择（开始-结束）
                Text(
                    text = "考试时间 *",
                    style = MaterialTheme.typography.labelMedium,
                    color = subtextColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 开始时间
                    TimePickerField(
                        hour = startHour,
                        minute = startMinute,
                        onTimeChange = { h, m -> startHour = h; startMinute = m },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )

                    Text("-", color = textColor, style = MaterialTheme.typography.titleMedium)

                    // 结束时间
                    TimePickerField(
                        hour = endHour,
                        minute = endMinute,
                        onTimeChange = { h, m -> endHour = h; endMinute = m },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 考试地点（必填）
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("考试地点 *") },
                    placeholder = { Text("如：A101教室", color = subtextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedLabelColor = subtextColor,
                        cursorColor = primaryColor
                    )
                )

                // 学分和考试类型（选填）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = credit,
                        onValueChange = { credit = it },
                        label = { Text("学分") },
                        placeholder = { Text("如：3", color = subtextColor) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedLabelColor = subtextColor,
                            cursorColor = primaryColor
                        )
                    )

                    OutlinedTextField(
                        value = examType,
                        onValueChange = { examType = it },
                        label = { Text("考试类型") },
                        placeholder = { Text("如：考试/考查", color = subtextColor) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedLabelColor = subtextColor,
                            cursorColor = primaryColor
                        )
                    )
                }

                // 学期信息（只读）
                Text(
                    text = "学期：${exam.yearSemester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedExam = exam.copy(
                        courseName = courseName.trim(),
                        time = formatTime(),
                        location = location.trim(),
                        credit = credit.trim(),
                        examType = examType.trim().ifEmpty { "考试" }
                    )
                    onSave(updatedExam)
                },
                enabled = courseName.isNotBlank() && location.isNotBlank()
            ) {
                Text(
                    "保存",
                    color = if (courseName.isNotBlank() && location.isNotBlank()) primaryColor else subtextColor
                )
            }
        },
        dismissButton = {
            Row {
                // 编辑模式下显示删除按钮
                if (!isAddMode) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = errorColor)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("取消", color = subtextColor)
                }
            }
        }
    )
}

// ============================================================================
// 日期时间选择器组件
// ============================================================================

/**
 * 考试日期选择器
 */
@Composable
private fun ExamDatePicker(
    currentDate: LocalDate,
    isDarkTheme: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val selectedBgColor =
        if (isDarkTheme) NightBlue.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.1f)

    var selectedYear by remember { mutableStateOf(currentDate.year) }
    var selectedMonth by remember { mutableStateOf(currentDate.monthNumber) }
    var selectedDay by remember { mutableStateOf(currentDate.dayOfMonth) }

    val monthNames = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )
    val weekDayNames = listOf("一", "二", "三", "四", "五", "六", "日")

    // 计算某月的天数
    fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            else -> 30
        }
    }

    // 获取某月第一天是星期几 (0=周一, 6=周日)
    fun firstDayOfMonth(year: Int, month: Int): Int {
        val date = LocalDate(year, month, 1)
        return date.dayOfWeek.ordinal
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)  // 限制最大宽度，防止平板上过大
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题
                Text(
                    "选择考试日期",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(Modifier.height(16.dp))

                // 选中日期显示
                Text(
                    "${selectedYear}年${monthNames[selectedMonth - 1]}${selectedDay}日",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 年月选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 年份选择
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = textColor)
                        }
                        Text(
                            "${selectedYear}年",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.ChevronRight, null, tint = textColor)
                        }
                    }

                    // 月份选择
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth > 1) selectedMonth--
                            else {
                                selectedMonth = 12; selectedYear--
                            }
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = textColor)
                        }
                        Text(
                            monthNames[selectedMonth - 1],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        IconButton(onClick = {
                            if (selectedMonth < 12) selectedMonth++
                            else {
                                selectedMonth = 1; selectedYear++
                            }
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(Icons.Default.ChevronRight, null, tint = textColor)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDayNames.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelMedium,
                                color = subtextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 日期网格
                val firstDay = firstDayOfMonth(selectedYear, selectedMonth)
                val daysCount = daysInMonth(selectedYear, selectedMonth)
                val totalCells = firstDay + daysCount
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val day = cellIndex - firstDay + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (day in 1..daysCount && day == selectedDay)
                                                selectedBgColor
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = day in 1..daysCount) {
                                            selectedDay = day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day in 1..daysCount) {
                                        Text(
                                            "$day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (day == selectedDay) primaryColor else textColor,
                                            fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = subtextColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(LocalDate(selectedYear, selectedMonth, selectedDay))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("确定", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 时间选择字段
 */
@Composable
private fun TimePickerField(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val bgColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            isDarkTheme = isDarkTheme,
            onTimeSelected = { h, m ->
                onTimeChange(h, m)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    Surface(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 时间选择器对话框
 */
@Composable
private fun TimePickerDialog(
    hour: Int,
    minute: Int,
    isDarkTheme: Boolean,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey

    var selectedHour by remember { mutableStateOf(hour) }
    var selectedMinute by remember { mutableStateOf(minute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "选择时间",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 时间选择器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 小时选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor)
                        }
                        Text(
                            selectedHour.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        IconButton(onClick = {
                            selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor)
                        }
                    }

                    Text(
                        ":",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 分钟选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor)
                        }
                        Text(
                            selectedMinute.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        IconButton(onClick = {
                            selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = subtextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTimeSelected(selectedHour, selectedMinute) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("确定", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 辅助解析函数
// ============================================================================

/**
 * 解析考试日期
 * 支持格式: "2025-01-15(09:00-11:00)" 或 "2025-01-15 09:00-11:00"
 */
private fun parseExamDate(timeStr: String): LocalDate? {
    return try {
        // 先尝试括号格式，再尝试空格格式
        val datePart = timeStr.substringBefore("(").takeIf { it != timeStr }
            ?: timeStr.split(" ").firstOrNull()
            ?: return null
        val parts = datePart.trim().split("-")
        if (parts.size >= 3) {
            LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * 提取时间部分（括号内或空格后的内容）
 */
private fun extractTimePart(timeStr: String): String? {
    // 尝试括号格式: "2025-01-15(09:00-11:00)"
    val bracketContent = timeStr.substringAfter("(", "").substringBefore(")", "")
    if (bracketContent.contains("-") && bracketContent.contains(":")) {
        return bracketContent
    }
    // 尝试空格格式: "2025-01-15 09:00-11:00"
    return timeStr.split(" ").getOrNull(1)
}

/**
 * 解析开始小时
 */
private fun parseExamStartHour(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val startTime = timePart.split("-").firstOrNull() ?: return null
        startTime.split(":").firstOrNull()?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析开始分钟
 */
private fun parseExamStartMinute(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val startTime = timePart.split("-").firstOrNull() ?: return null
        startTime.split(":").getOrNull(1)?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析结束小时
 */
private fun parseExamEndHour(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val endTime = timePart.split("-").getOrNull(1) ?: return null
        endTime.split(":").firstOrNull()?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析结束分钟
 */
private fun parseExamEndMinute(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val endTime = timePart.split("-").getOrNull(1) ?: return null
        endTime.split(":").getOrNull(1)?.toInt()
    } catch (e: Exception) {
        null
    }
}
