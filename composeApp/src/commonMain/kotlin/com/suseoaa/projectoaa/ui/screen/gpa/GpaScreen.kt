package com.suseoaa.projectoaa.ui.screen.gpa

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.shared.data.repository.GpaCourseWrapper
import com.suseoaa.projectoaa.presentation.gpa.GpaViewModel
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.presentation.gpa.FilterType
import com.suseoaa.projectoaa.presentation.gpa.SortOrder
import com.suseoaa.projectoaa.ui.component.BackButton
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.pow
import kotlin.math.round

@Immutable
private data class GpaCourseUiModel(
    val courseId: String,
    val termCode: String,
    val courseName: String,
    val isDegreeCourse: Boolean,
    val isGradeLevel: Boolean,
    val isPassOnly: Boolean,
    val creditText: String,
    val displayScore: String,
    val displayGpa: String
)

private fun Double.toFixed(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    val raw = rounded.toString().split(".")
    return if (raw.size == 1) {
        "${raw[0]}.${"0".repeat(decimals)}"
    } else {
        val decimalsPart = raw[1].padEnd(decimals, '0').take(decimals)
        "${raw[0]}.$decimalsPart"
    }
}

private fun GpaCourseWrapper.toUiModel(): GpaCourseUiModel {
    return GpaCourseUiModel(
        courseId = originalEntity.courseId,
        termCode = "${originalEntity.xnm}_${originalEntity.xqm}",
        courseName = originalEntity.courseName,
        isDegreeCourse = isDegreeCourse,
        isGradeLevel = isGradeLevel,
        isPassOnly = isPassOnly,
        creditText = credit.toFixed(1),
        displayScore = displayScore,
        displayGpa = displayGpa
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaScreen(
    onBack: () -> Unit,
    viewModel: GpaViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.courseList.isEmpty()) {
            viewModel.loadData()
        }
    }

    Scaffold(
        modifier = Modifier.sharedBoundsTransition("gpa"),
        topBar = {
            TopAppBar(
                title = { Text("绩点计算") },
                navigationIcon = {
                    BackButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            uiState.errorMessage ?: "发生错误",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("重试")
                        }
                    }
                }

                else -> {
                    GpaContent(
                        courseList = uiState.courseList,
                        totalGpa = uiState.totalGpa,
                        totalCredits = uiState.totalCredits,
                        degreeGpa = uiState.degreeGpa,
                        degreeCredits = uiState.degreeCredits,
                        sortOrder = uiState.sortOrder,
                        filterType = uiState.filterType,
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        onScoreChange = { courseId, score ->
                            viewModel.updateSimulatedScoreByCourseId(courseId, score)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GpaContent(
    courseList: List<GpaCourseWrapper>,
    totalGpa: String,
    totalCredits: String,
    degreeGpa: String,
    degreeCredits: String,
    sortOrder: SortOrder,
    filterType: FilterType,
    onSortOrderChange: (SortOrder) -> Unit,
    onFilterTypeChange: (FilterType) -> Unit,
    onScoreChange: (String, Double) -> Unit
) {
    val courseUiList by remember(courseList) {
        derivedStateOf { courseList.map { it.toUiModel() } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 顶部统计卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("总绩点", totalGpa, totalCredits)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                )
                StatItem("学位绩点", degreeGpa, degreeCredits)
            }
        }

        // 2. 筛选和排序操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 筛选分类
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == FilterType.ALL,
                    onClick = { onFilterTypeChange(FilterType.ALL) },
                    label = { Text("全部") },
                    leadingIcon = if (filterType == FilterType.ALL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == FilterType.DEGREE_ONLY,
                    onClick = { onFilterTypeChange(FilterType.DEGREE_ONLY) },
                    label = { Text("学位课") },
                    leadingIcon = if (filterType == FilterType.DEGREE_ONLY) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }

            // 排序按钮
            TextButton(
                onClick = {
                    val newOrder = if (sortOrder == SortOrder.DESCENDING)
                        SortOrder.ASCENDING else SortOrder.DESCENDING
                    onSortOrderChange(newOrder)
                }
            ) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.DESCENDING)
                        Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (sortOrder == SortOrder.DESCENDING) "从高到低" else "从低到高")
            }
        }

        Text(
            "点击课程修改成绩进行模拟",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 3. 课程列表
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(modifier = Modifier.weight(1f)) {
            AdaptiveLayout { config ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.getListColumns()),
                    contentPadding = PaddingValues(
                        start = config.horizontalPadding,
                        end = config.horizontalPadding,
                        bottom = 16.dp + navBarHeight,
                        top = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = courseUiList,
                        key = { "${it.courseId}_${it.termCode}" },
                        contentType = { "gpa_course_item" }
                    ) { item ->
                        GpaCourseItem(
                            item = item,
                            onScoreChange = onScoreChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, gpa: String, credit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = gpa,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "共 " + credit + " 学分",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CourseTag(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GpaCourseItem(
    item: GpaCourseUiModel,
    onScoreChange: (String, Double) -> Unit
) {
    var showDialog by remember(item.courseId, item.termCode) { mutableStateOf(false) }

    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = if (item.isDegreeCourse) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isDegreeCourse) {
                        CourseTag(
                            text = "学位课",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (item.isGradeLevel) {
                        CourseTag(
                            text = "等级制",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (item.isPassOnly) {
                        CourseTag(
                            text = "通过制",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "${item.creditText} 学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.displayScore,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "GPA: ${item.displayGpa}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    if (showDialog) {
        EditScoreDialog(
            initialScore = item.displayScore,
            isGradeLevel = item.isGradeLevel,
            onDismiss = { showDialog = false },
            onConfirm = { scoreStr ->
                scoreStr.toDoubleOrNull()?.let { score ->
                    onScoreChange(item.courseId, score)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun EditScoreDialog(
    initialScore: String,
    isGradeLevel: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(if (isGradeLevel) "" else initialScore) }
    var selectedGrade by remember { mutableStateOf<String?>(if (isGradeLevel) initialScore else null) }

    // 等级制成绩选项及对应的分数
    val gradeOptions = listOf(
        "优" to "95",
        "良" to "85",
        "中" to "75",
        "及格" to "65",
        "差" to "55"
    )

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("修改模拟成绩") },
        text = {
            Column {
                // 等级制成绩快捷选择
                Text(
                    "等级制成绩 (点击选择):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gradeOptions.forEach { (grade, score) ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = {
                                selectedGrade = grade
                                text = score
                            },
                            label = { Text(grade, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "或直接输入分数:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 3) {
                            text = it
                            selectedGrade = null  // 清除等级选择
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("分数 (0-100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
