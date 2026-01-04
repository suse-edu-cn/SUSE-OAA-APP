package com.suseoaa.projectoaa.feature.academicPortal.getGrades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: GradesViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // 1. 收集状态
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val isRefreshing = viewModel.isRefreshing

    // 2. 处理 SnackBar 消息
    val snackbarHostState = remember { SnackbarHostState() }
    val message = viewModel.refreshMessage
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 3. 计算入学年份 (用于生成下拉框)
    // 如果数据库还没查到入学年份，默认显示最近4年
    val startYear = remember(currentAccount) {
        currentAccount?.njdmId?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.YEAR) - 4)
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
                // 右上角刷新按钮
                actions = {
                    IconButton(onClick = { viewModel.refreshGrades() }, enabled = !isRefreshing) {
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
            // 4. 筛选器组件 (无状态，只负责显示和回调)
            SelectOption(
                selectedYear = viewModel.selectedXnm,
                selectedSemester = viewModel.selectedXqm,
                startYear = startYear,
                onFilterChange = { year, semester ->
                    viewModel.updateFilter(year, semester)
                }
            )

            // 5. 列表内容
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
                                Text("点击右上角按钮刷新")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grades) { item ->
                        GradeItemCard(item)
                    }
                }
            }
        }
    }
}

// 下拉框组件
@Composable
fun SelectOption(
    selectedYear: String,
    selectedSemester: String,
    startYear: Int,
    onFilterChange: (String, String) -> Unit
) {
    // 动态生成年份列表：从入学年 -> 当前年+1
    val yearOptions = remember(startYear) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val endYear = currentYear + 1
        val list = mutableListOf<Pair<String, String>>()
        // 倒序：最近的年份在最上面
        for (y in endYear downTo startYear) {
            list.add("$y-${y + 1} 学年" to y.toString())
        }
        list
    }

    val semesterOptions = listOf("上学期" to "3", "下学期" to "12")

    // 获取当前显示的文字
    val currentYearLabel = yearOptions.find { it.second == selectedYear }?.first ?: "${selectedYear}学年"
    val currentSemesterLabel = semesterOptions.find { it.second == selectedSemester }?.first ?: "未知学期"

    var expandedYear by remember { mutableStateOf(false) }
    var expandedSemester by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // 年份
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

        // 学期
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

// 统一按钮样式
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
            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// 卡片组件 (直接使用 Entity)
@Composable
fun GradeItemCard(item: GradeEntity) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
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
        }
    }
}

@Composable
fun LabelValueText(label: String, value: String?) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}