package com.suseoaa.projectoaa.ui.screen.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.grades.GradesViewModel
import com.suseoaa.projectoaa.data.repository.GradeEntity
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.component.BackButton
import com.suseoaa.projectoaa.ui.component.getListColumns
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.showToast
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    onBack: () -> Unit,
    viewModel: GradesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 显示Toast消息
    uiState.message?.let { message ->
        showToast(message)
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成绩查询") },
                navigationIcon = {
                    BackButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshGrades() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        AdaptiveLayout { config ->
            if (config.useSideNavigation) {
                // 平板横屏：左右布局
                TabletGradesLayout(
                    uiState = uiState,
                    config = config,
                    onFilterChange = viewModel::updateFilter,
                    onRefresh = viewModel::refreshGrades,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                // 手机/平板竖屏：传统布局
                PhoneGradesLayout(
                    uiState = uiState,
                    config = config,
                    onFilterChange = viewModel::updateFilter,
                    onRefresh = viewModel::refreshGrades,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/**
 * 平板横屏布局：左侧筛选面板 + 右侧成绩列表
 */
@Composable
private fun BoxWithConstraintsScope.TabletGradesLayout(
    uiState: com.suseoaa.projectoaa.presentation.grades.GradesUiState,
    config: AdaptiveLayoutConfig,
    onFilterChange: (String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    
    Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // 左侧筛选面板 - 使用Card圆角包裹
        Card(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "筛选条件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                VerticalFilterSection(
                    selectedYear = uiState.selectedYear,
                    selectedSemester = uiState.selectedSemester,
                    startYear = uiState.startYear,
                    onFilterChange = onFilterChange
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 统计信息
                if (uiState.grades.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "共 ${uiState.grades.size} 门课程",
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtextColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 右侧内容区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            GradesContent(
                uiState = uiState,
                config = config,
                onRefresh = onRefresh
            )
        }
    }
}

/**
 * 手机/平板竖屏布局
 */
@Composable
private fun BoxWithConstraintsScope.PhoneGradesLayout(
    uiState: com.suseoaa.projectoaa.presentation.grades.GradesUiState,
    config: AdaptiveLayoutConfig,
    onFilterChange: (String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 筛选栏
        SelectOption(
            selectedYear = uiState.selectedYear,
            selectedSemester = uiState.selectedSemester,
            startYear = uiState.startYear,
            onFilterChange = onFilterChange
        )

        // 内容区
        GradesContent(
            uiState = uiState,
            config = config,
            onRefresh = onRefresh
        )
    }
}

/**
 * 成绩列表内容
 */
@Composable
private fun GradesContent(
    uiState: com.suseoaa.projectoaa.presentation.grades.GradesUiState,
    config: AdaptiveLayoutConfig,
    onRefresh: () -> Unit
) {
    when {
        uiState.grades.isEmpty() && !uiState.isRefreshing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无本地数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRefresh) {
                        Text("点击右上角刷新")
                    }
                }
            }
        }
        uiState.grades.isEmpty() && uiState.isRefreshing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "正在同步所有学期成绩...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val columns = config.getListColumns()
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = config.horizontalPadding,
                    end = config.horizontalPadding,
                    top = 8.dp,
                    bottom = 16.dp + navBarHeight
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.grades, key = { "${it.studentId}_${it.courseId}_${it.xnm}_${it.xqm}" }) { item ->
                    GradeItemCard(item)
                }
            }
        }
    }
}

/**
 * 垂直筛选区域（平板左侧面板用）- 使用自定义配色
 */
@Composable
private fun VerticalFilterSection(
    selectedYear: String,
    selectedSemester: String,
    startYear: Int,
    onFilterChange: (String, String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val selectedBgColor = if (isDarkTheme) NightContainer else SoftBlueWait
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    val yearOptions = remember(startYear) {
        val endYear = currentYear + 1
        val list = mutableListOf<Pair<String, String>>()
        for (y in endYear downTo startYear) {
            list.add("$y-${y + 1}" to y.toString())
        }
        list
    }
    val semesterOptions = listOf("上学期" to "3", "下学期" to "12")
    
    // 学年选择
    Text(
        text = "学年",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = subtextColor,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        yearOptions.forEach { (label, value) ->
            val isSelected = selectedYear == value
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(value, selectedSemester) },
                label = { 
                    Text(
                        label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedBgColor,
                    selectedLabelColor = primaryColor,
                    labelColor = textColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f),
                    selectedBorderColor = primaryColor.copy(alpha = 0.3f),
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
    
    Spacer(modifier = Modifier.height(28.dp))
    
    // 学期选择
    Text(
        text = "学期",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = subtextColor,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        semesterOptions.forEach { (label, value) ->
            val isSelected = selectedSemester == value
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(selectedYear, value) },
                label = { 
                    Text(
                        label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedBgColor,
                    selectedLabelColor = primaryColor,
                    labelColor = textColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f),
                    selectedBorderColor = primaryColor.copy(alpha = 0.3f),
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun SelectOption(
    selectedYear: String,
    selectedSemester: String,
    startYear: Int,
    onFilterChange: (String, String) -> Unit
) {
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    val yearOptions = remember(startYear) {
        val endYear = currentYear + 1
        val list = mutableListOf<Pair<String, String>>()
        for (y in endYear downTo startYear) {
            list.add("$y-${y + 1} 学年" to y.toString())
        }
        list
    }

    val semesterOptions = listOf("上学期" to "3", "下学期" to "12")
    val currentYearLabel = yearOptions.find { it.second == selectedYear }?.first ?: "${selectedYear}学年"
    val currentSemesterLabel = semesterOptions.find { it.second == selectedSemester }?.first ?: "未知学期"

    var expandedYear by remember { mutableStateOf(false) }
    var expandedSemester by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            FilterButton(text = currentYearLabel, onClick = { expandedYear = true })
            DropdownMenu(
                expanded = expandedYear,
                onDismissRequest = { expandedYear = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                yearOptions.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expandedYear = false
                            onFilterChange(value, selectedSemester)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            FilterButton(text = currentSemesterLabel, onClick = { expandedSemester = true })
            DropdownMenu(
                expanded = expandedSemester,
                onDismissRequest = { expandedSemester = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                semesterOptions.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expandedSemester = false
                            onFilterChange(selectedYear, value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradeItemCard(item: GradeEntity) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) NightSurface else OxygenWhite
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else InkGrey.copy(alpha = 0.2f)
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 第一行：课程名 和 总成绩
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.score,
                    style = MaterialTheme.typography.titleLarge,
                    color = getGradeColor(item.score),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 第二行：核心信息 - 使用FlowRow风格的自适应布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GradeInfoChip("学分", item.credit, subtextColor, textColor)
                    GradeInfoChip("绩点", item.gpa, subtextColor, textColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GradeInfoChip("类型", item.courseType, subtextColor, textColor)
                    GradeInfoChip("考核", item.examType, subtextColor, textColor)
                }
            }

            // 成绩详情（如果有）
            if (item.regularScore.isNotEmpty() || item.finalScore.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = dividerColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item.regularScore.isNotEmpty()) {
                        val regularLabel = if (item.regularRatio.isNotEmpty()) 
                            "平时(${item.regularRatio})" else "平时"
                        GradeInfoChip(regularLabel, item.regularScore, subtextColor, textColor)
                    }
                    if (item.experimentScore.isNotEmpty()) {
                        val expLabel = if (item.experimentRatio.isNotEmpty())
                            "实验(${item.experimentRatio})" else "实验"
                        GradeInfoChip(expLabel, item.experimentScore, subtextColor, textColor)
                    }
                    if (item.finalScore.isNotEmpty()) {
                        val finalLabel = if (item.finalRatio.isNotEmpty())
                            "期末(${item.finalRatio})" else "期末"
                        GradeInfoChip(finalLabel, item.finalScore, subtextColor, textColor)
                    }
                }
            }

            // 底部信息行
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = dividerColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 教师 - 最多显示4个字
                GradeInfoChip("教师", item.teacher?.take(4)?.let { 
                    if ((item.teacher?.length ?: 0) > 4) "$it…" else it 
                }, subtextColor, textColor)

                // 课程ID - 最多显示9位
                GradeInfoChip("课程号", item.courseId?.take(9)?.let {
                    if ((item.courseId?.length ?: 0) > 9) "$it…" else it
                }, subtextColor, textColor)

                // 考试性质
                GradeInfoChip("性质", item.examNature, subtextColor, textColor)
            }
        }
    }
}

/**
 * 成绩信息小标签 - 统一样式
 */
@Composable
private fun GradeInfoChip(
    label: String,
    value: String?,
    labelColor: Color,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LabelValueText(
    label: String, 
    value: String?,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 根据成绩返回对应的颜色
 */
fun getGradeColor(score: String): Color {
    val scoreVal = score.toDoubleOrNull()
    if (scoreVal != null) {
        return when {
            scoreVal >= 90 -> Color(0xFF2E7D32) // 深绿色 (优秀)
            scoreVal >= 80 -> Color(0xFF1565C0) // 深蓝色 (良好)
            scoreVal >= 70 -> Color(0xFF0097A7) // 青色 (中等)
            scoreVal >= 60 -> Color(0xFFEF6C00) // 橙色 (及格)
            else -> Color(0xFFC62828)           // 红色 (不及格)
        }
    }

    return when (score.trim()) {
        "优秀", "优" -> Color(0xFF2E7D32)
        "良好", "良" -> Color(0xFF1565C0)
        "中等", "中" -> Color(0xFF0097A7)
        "及格", "合格" -> Color(0xFFEF6C00)
        "不及格", "不合格" -> Color(0xFFC62828)
        else -> Color.Gray
    }
}
