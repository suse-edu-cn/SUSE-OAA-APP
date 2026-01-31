package com.suseoaa.projectoaa.ui.screen.gpa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.suseoaa.projectoaa.data.repository.GpaCourseWrapper
import com.suseoaa.projectoaa.presentation.gpa.GpaViewModel
import com.suseoaa.projectoaa.presentation.gpa.FilterType
import com.suseoaa.projectoaa.presentation.gpa.SortOrder
import com.suseoaa.projectoaa.ui.component.BackButton
import org.koin.compose.viewmodel.koinViewModel

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
                        onScoreChange = { item, score -> viewModel.updateSimulatedScore(item, score) }
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
    onScoreChange: (GpaCourseWrapper, Double) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 顶部统计卡片
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("总绩点", totalGpa, totalCredits)
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
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp + navBarHeight
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(courseList, key = { it.originalEntity.courseName }) { item ->
                GpaCourseItem(
                    item = item,
                    onScoreChange = { newScore ->
                        onScoreChange(item, newScore)
                    }
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, gpa: String, credit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            gpa,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "总学分: $credit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun GpaCourseItem(
    item: GpaCourseWrapper,
    onScoreChange: (Double) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val containerColor = if (item.isDegreeCourse) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.originalEntity.courseName,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isDegreeCourse) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "学位课",
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // 如果是等级制成绩，显示等级标签
                    if (item.isGradeLevel) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "等级制",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // 如果是仅通过类成绩（合格/通过/免修），显示标签
                    if (item.isPassOnly) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "通过制",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        "学分: ${item.credit}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.displayScore,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "GPA: ${item.displayGpa}",
                    style = MaterialTheme.typography.bodySmall
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
                    onScoreChange(score)
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
