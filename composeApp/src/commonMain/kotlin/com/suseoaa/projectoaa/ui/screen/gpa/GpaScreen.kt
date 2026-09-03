package com.suseoaa.projectoaa.ui.screen.gpa

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
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
import com.suseoaa.projectoaa.presentation.gpa.FilterType
import com.suseoaa.projectoaa.presentation.gpa.SortOrder
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
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
    val displayGpa: String,
    val isIncludedInCalculation: Boolean
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
        courseId = originalEntity.courseId.ifEmpty { originalEntity.courseName },
        termCode = "${originalEntity.xnm}_${originalEntity.xqm}",
        courseName = originalEntity.courseName,
        isDegreeCourse = isDegreeCourse,
        isGradeLevel = isGradeLevel,
        isPassOnly = isPassOnly,
        creditText = credit.toFixed(1),
        displayScore = displayScore,
        displayGpa = displayGpa,
        isIncludedInCalculation = isIncludedInCalculation
    )
}

/** 将学期代码转换为易读文本，例如 2023_3 -> 2023-2024 第1学期 */
private fun formatTerm(termCode: String): String {
    if (termCode == "ALL") return "全部学期"
    val parts = termCode.split("_")
    if (parts.size == 2) {
        val year = parts[0]
        val nextYear = (year.toIntOrNull() ?: 0) + 1
        val termStr = when (parts[1]) {
            "3" -> "第1学期"
            "12" -> "第2学期"
            "16" -> "第3学期"
            else -> "第${parts[1]}学期"
        }
        return "$year-$nextYear $termStr"
    }
    return termCode
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

    AdaptivePageScaffold(
        title = "绩点计算",
        onBack = onBack,
        sharedTransitionKey = "gpa"
    ) { contentModifier ->
        Box(modifier = contentModifier) {
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
                        termList = uiState.termList,
                        selectedTerm = uiState.selectedTerm,
                        totalGpa = uiState.totalGpa,
                        totalCredits = uiState.totalCredits,
                        degreeGpa = uiState.degreeGpa,
                        degreeCredits = uiState.degreeCredits,
                        sortOrder = uiState.sortOrder,
                        filterType = uiState.filterType,
                        onTermChange = { viewModel.setTermFilter(it) },
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        onScoreChange = { courseId, score ->
                            viewModel.updateSimulatedScoreByCourseId(courseId, score)
                        },
                        onInclusionChange = { courseId, isIncluded ->
                            viewModel.updateCourseInclusion(courseId, isIncluded)
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
    termList: List<String>,
    selectedTerm: String,
    totalGpa: String,
    totalCredits: String,
    degreeGpa: String,
    degreeCredits: String,
    sortOrder: SortOrder,
    filterType: FilterType,
    onTermChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onFilterTypeChange: (FilterType) -> Unit,
    onScoreChange: (String, Double) -> Unit,
    onInclusionChange: (String, Boolean) -> Unit
) {
    val courseUiList by remember(courseList) {
        derivedStateOf { courseList.map { it.toUiModel() } }
    }
    val includedList = courseUiList.filter { it.isIncludedInCalculation }
    val excludedList = courseUiList.filter { !it.isIncludedInCalculation }
    val allTerms = listOf("ALL") + termList
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AdaptiveLayout { config ->
        if (config.isTablet) {
            // 平板双栏仪表盘布局
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = navBarHeight)
            ) {
                // 左侧控制台
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 8.dp)
                ) {
                    DashboardSummaryCard(totalGpa, totalCredits, degreeGpa, degreeCredits)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 学期列表作为侧边导航
                    Text(
                        "学期筛选",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lazyItems(allTerms) { term ->
                            val isSelected = selectedTerm == term
                            Surface(
                                onClick = { onTermChange(term) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTerm(term),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    // 底部控制
                    GpaFilterControls(filterType, sortOrder, onFilterTypeChange, onSortOrderChange)
                }

                // 右侧课程列表
                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, top = 8.dp, bottom = 16.dp, end = 16.dp)
                ) {
                    Text(
                        "点击课程修改成绩进行模拟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(config.getListColumns()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (includedList.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "参与计算的课程",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = includedList,
                                key = { "${it.courseId}_${it.termCode}" },
                                contentType = { "gpa_course_item" }
                            ) { item ->
                                GpaCourseItem(item = item, onScoreChange = onScoreChange, onInclusionChange = onInclusionChange)
                            }
                        }
                        if (excludedList.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "不参与计算的课程",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = excludedList,
                                key = { "${it.courseId}_${it.termCode}" },
                                contentType = { "gpa_course_item" }
                            ) { item ->
                                GpaCourseItem(item = item, onScoreChange = onScoreChange, onInclusionChange = onInclusionChange)
                            }
                        }
                    }
                }
            }
        } else {
            // 手机单列布局
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 学期选择 (横向滑动)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lazyItems(allTerms) { term ->
                        FilterChip(
                            selected = selectedTerm == term,
                            onClick = { onTermChange(term) },
                            label = { Text(formatTerm(term)) },
                            leadingIcon = if (selectedTerm == term) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // 2. 顶部统计卡片
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DashboardSummaryCard(totalGpa, totalCredits, degreeGpa, degreeCredits)
                }

                // 3. 筛选和排序操作栏
                GpaFilterControls(
                    filterType = filterType,
                    sortOrder = sortOrder,
                    onFilterTypeChange = onFilterTypeChange,
                    onSortOrderChange = onSortOrderChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Text(
                    "点击课程修改成绩进行模拟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 4. 课程列表
                Box(modifier = Modifier.weight(1f)) {
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
                        if (includedList.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "参与计算的课程",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = includedList,
                                key = { "${it.courseId}_${it.termCode}" },
                                contentType = { "gpa_course_item" }
                            ) { item ->
                                GpaCourseItem(item = item, onScoreChange = onScoreChange, onInclusionChange = onInclusionChange)
                            }
                        }
                        if (excludedList.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "不参与计算的课程",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = excludedList,
                                key = { "${it.courseId}_${it.termCode}" },
                                contentType = { "gpa_course_item" }
                            ) { item ->
                                GpaCourseItem(item = item, onScoreChange = onScoreChange, onInclusionChange = onInclusionChange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSummaryCard(totalGpa: String, totalCredits: String, degreeGpa: String, degreeCredits: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(20.dp)
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
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
            StatItem("学位绩点", degreeGpa, degreeCredits)
        }
    }
}

@Composable
private fun GpaFilterControls(
    filterType: FilterType,
    sortOrder: SortOrder,
    onFilterTypeChange: (FilterType) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 筛选分类
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterType == FilterType.ALL,
                onClick = { onFilterTypeChange(FilterType.ALL) },
                label = { Text("全部") },
            )
            FilterChip(
                selected = filterType == FilterType.DEGREE_ONLY,
                onClick = { onFilterTypeChange(FilterType.DEGREE_ONLY) },
                label = { Text("学位课") },
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
    onScoreChange: (String, Double) -> Unit,
    onInclusionChange: (String, Boolean) -> Unit
) {
    var showDialog by remember(item.courseId, item.termCode) { mutableStateOf(false) }

    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = if (item.isDegreeCourse) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "GPA: ${item.displayGpa}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
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
            isIncluded = item.isIncludedInCalculation,
            onDismiss = { showDialog = false },
            onConfirm = { scoreStr, isIncluded ->
                scoreStr.toDoubleOrNull()?.let { score ->
                    onScoreChange(item.courseId, score)
                }
                onInclusionChange(item.courseId, isIncluded)
                showDialog = false
            }
        )
    }
}

@Composable
fun EditScoreDialog(
    initialScore: String,
    isGradeLevel: Boolean = false,
    isIncluded: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(if (isGradeLevel) "" else initialScore) }
    var selectedGrade by remember { mutableStateOf<String?>(if (isGradeLevel) initialScore else null) }
    var includedState by remember { mutableStateOf(isIncluded) }

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
                // 纳入计算开关
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("参与绩点计算", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = includedState,
                        onCheckedChange = { includedState = it },
                        colors = SwitchDefaults.colors(
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
                
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
            Button(onClick = { onConfirm(text, includedState) }) {
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
