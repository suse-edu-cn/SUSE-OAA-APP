package com.suseoaa.projectoaa.feature.academicPortal.getGrades

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    windowSizeClass: WindowWidthSizeClass,
    viewModel: GradesViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val isRefreshing = viewModel.isRefreshing

    val snackbarHostState = remember { SnackbarHostState() }
    val message = viewModel.refreshMessage
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val startYear = remember(currentAccount) {
        currentAccount?.njdmId?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.YEAR) - 4)
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "academic_grades_card"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = AcademicSharedTransitionSpec,
                    zIndexInOverlay = 1f
                ),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("成绩查询") },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshGrades() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "全量更新")
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
                SelectOption(
                    selectedYear = viewModel.selectedXnm,
                    selectedSemester = viewModel.selectedXqm,
                    startYear = startYear,
                    onFilterChange = { year, semester ->
                        viewModel.updateFilter(year, semester)
                    }
                )

                if (grades.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isRefreshing) "正在同步所有学期成绩..." else "暂无本地数据",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isRefreshing) {
                                TextButton(onClick = { viewModel.refreshGrades() }) {
                                    Text("点击右上角刷新")
                                }
                            }
                        }
                    }
                } else {
                    val columns = if (windowSizeClass == WindowWidthSizeClass.Compact) 1 else 2

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(grades) { item ->
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
    val yearOptions = remember(startYear) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val endYear = currentYear + 1
        val list = mutableListOf<Pair<String, String>>()
        for (y in endYear downTo startYear) {
            list.add("$y-${y + 1} 学年" to y.toString())
        }
        list
    }

    val semesterOptions = listOf("上学期" to "3", "下学期" to "12")
    val currentYearLabel =
        yearOptions.find { it.second == selectedYear }?.first ?: "${selectedYear}学年"
    val currentSemesterLabel =
        semesterOptions.find { it.second == selectedSemester }?.first ?: "未知学期"

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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.score,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 平时成绩 + 比例
                val regularLabel =
                    if (item.regularRatio.isNotBlank()) "平时(${item.regularRatio})" else "平时"
                LabelValueText(
                    label = regularLabel,
                    value = item.regularScore.ifBlank { "-" }
                )

                // 期末成绩 + 比例
                val finalLabel =
                    if (item.finalRatio.isNotBlank()) "期末(${item.finalRatio})" else "期末"
                LabelValueText(
                    label = finalLabel,
                    value = item.finalScore.ifBlank { "-" }
                )
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