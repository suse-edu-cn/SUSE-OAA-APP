package com.suseoaa.projectoaa.ui.screen.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("成绩查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 筛选栏
            SelectOption(
                selectedYear = uiState.selectedYear,
                selectedSemester = uiState.selectedSemester,
                startYear = uiState.startYear,
                onFilterChange = { year, semester ->
                    viewModel.updateFilter(year, semester)
                }
            )

            // 内容区
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
                            TextButton(onClick = { viewModel.refreshGrades() }) {
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
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.score,
                    style = MaterialTheme.typography.titleLarge,
                    color = getGradeColor(item.score),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：学分等信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    LabelValueText("学分", item.credit)
                    LabelValueText("绩点", item.gpa)
                }
                Column(horizontalAlignment = Alignment.End) {
                    LabelValueText("类型", item.courseType)
                    LabelValueText("考核", item.examType)
                }
            }

            // 第三行：分割线 + 详情
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 成绩详情行（如果有）
            if (item.regularScore.isNotEmpty() || item.finalScore.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item.regularScore.isNotEmpty()) {
                        val regularLabel = if (item.regularRatio.isNotEmpty()) 
                            "平时(${item.regularRatio})" else "平时"
                        LabelValueText(regularLabel, item.regularScore)
                    }
                    if (item.experimentScore.isNotEmpty()) {
                        val expLabel = if (item.experimentRatio.isNotEmpty())
                            "实验(${item.experimentRatio})" else "实验"
                        LabelValueText(expLabel, item.experimentScore)
                    }
                    if (item.finalScore.isNotEmpty()) {
                        val finalLabel = if (item.finalRatio.isNotEmpty())
                            "期末(${item.finalRatio})" else "期末"
                        LabelValueText(finalLabel, item.finalScore)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 教师
                LabelValueText("教师", item.teacher)

                // 课程ID
                LabelValueText("课程号", item.courseId)

                // 考试性质
                LabelValueText("性质", item.examNature)
            }
        }
    }
}

@Composable
fun LabelValueText(label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
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
