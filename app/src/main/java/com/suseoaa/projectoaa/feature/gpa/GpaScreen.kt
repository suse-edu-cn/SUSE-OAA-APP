package com.suseoaa.projectoaa.feature.gpa

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaScreen(
    windowSizeClass: WindowWidthSizeClass,
    viewModel: GpaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val courseList by viewModel.courseList.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    LaunchedEffect(Unit) {
        if (courseList.isEmpty()) viewModel.loadData()
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "academic_gpa_card"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = AcademicSharedTransitionSpec,
                zIndexInOverlay = 1f
            ),
            topBar = {
                TopAppBar(
                    title = { Text("绩点计算") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("重试")
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. 顶部统计卡片
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                                StatItem("总绩点", stats.totalGpa, stats.totalCredits)
                                StatItem("学位绩点", stats.degreeGpa, stats.degreeCredits)
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
                                    onClick = { viewModel.setFilterType(FilterType.ALL) },
                                    label = { Text("全部") },
                                    leadingIcon = if (filterType == FilterType.ALL) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = filterType == FilterType.DEGREE_ONLY,
                                    onClick = { viewModel.setFilterType(FilterType.DEGREE_ONLY) },
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
                                    viewModel.setSortOrder(newOrder)
                                }
                            ) {
                                Icon(
                                    imageVector = if (sortOrder == SortOrder.DESCENDING)
                                        Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
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
                        val columns = if (windowSizeClass == WindowWidthSizeClass.Compact) 1 else 2

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(courseList, key = { it.originalEntity.courseId }) { item ->
                                GpaCourseItem(
                                    item = item,
                                    onScoreChange = { newScore ->
                                        viewModel.updateSimulatedScore(item, newScore)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, gpa: String, credit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(gpa, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("总学分: $credit", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun GpaCourseItem(
    item: GpaCourseWrapper,
    onScoreChange: (Double) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // === 修复的核心：使用实色（Remove Alpha）并添加 Elevation ===
    val containerColor = if (item.isDegreeCourse) {
        // 学位课：使用 TertiaryContainer 实色
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        // 普通课：使用 SurfaceContainerHighest (或 SurfaceVariant)
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        // 添加阴影
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.originalEntity.courseName, fontWeight = FontWeight.Bold)
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
                    Text(
                        "学分: ${item.originalEntity.credit}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.simulatedScore.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "GPA: ${"%.2f".format(item.simulatedGpa)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showDialog) {
        EditScoreDialog(
            initialScore = item.simulatedScore.toString(),
            onDismiss = { showDialog = false },
            onConfirm = {
                it.toDoubleOrNull()?.let { s -> onScoreChange(s) }
                showDialog = false
            }
        )
    }
}

@Composable
fun EditScoreDialog(initialScore: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialScore) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("修改模拟成绩") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 3) text = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("分数") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}