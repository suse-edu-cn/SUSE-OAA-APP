package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.data.model.CourseInfoItem
import com.suseoaa.projectoaa.presentation.teachingplan.CourseInfoViewModel
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/**
 * 课程信息查询界面
 * 只能查询当前学生自己专业的课程信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseInfoScreen(
    onBack: () -> Unit,
    viewModel: CourseInfoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val availableCourseTypes by viewModel.availableCourseTypes.collectAsState()
    val focusManager = LocalFocusManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程信息查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 筛选按钮（显示筛选状态）
                    val hasActiveFilters = uiState.selectedYear.isNotEmpty() || 
                                          uiState.selectedSemester.isNotEmpty() ||
                                          uiState.selectedCourseType.isNotEmpty() ||
                                          uiState.searchKeyword.isNotEmpty()
                    IconButton(onClick = { viewModel.toggleFilterExpanded() }) {
                        BadgedBox(
                            badge = {
                                if (hasActiveFilters) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        ) {
                            Icon(
                                if (uiState.isFilterExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.AutoMirrored.Filled.List,
                                contentDescription = if (uiState.isFilterExpanded) "收起筛选" else "展开筛选"
                            )
                        }
                    }
                    // 刷新按钮
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 可折叠搜索和筛选区域
            CollapsibleSearchAndFilterSection(
                isExpanded = uiState.isFilterExpanded,
                onToggleExpand = { viewModel.toggleFilterExpanded() },
                searchKeyword = uiState.searchKeyword,
                onSearchChange = viewModel::setSearchKeyword,
                onSearch = { focusManager.clearFocus() },
                selectedYear = uiState.selectedYear,
                selectedSemester = uiState.selectedSemester,
                selectedCourseType = uiState.selectedCourseType,
                availableYears = availableYears,
                availableCourseTypes = availableCourseTypes,
                onYearSelect = viewModel::setYearFilter,
                onSemesterSelect = viewModel::setSemesterFilter,
                onCourseTypeSelect = viewModel::setCourseTypeFilter,
                onClearFilters = viewModel::clearFilters,
                hasActiveFilters = uiState.selectedYear.isNotEmpty() || 
                                   uiState.selectedSemester.isNotEmpty() ||
                                   uiState.selectedCourseType.isNotEmpty() ||
                                   uiState.searchKeyword.isNotEmpty()
            )
            
            // 错误提示 - 使用 Toast
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    ToastManager.showToast(error)
                    viewModel.clearError()
                }
            }
            
            // 统计信息
            if (uiState.filteredCourses.isNotEmpty()) {
                StatisticsBar(
                    totalCourses = uiState.filteredCourses.size,
                    totalCredits = viewModel.getTotalCredits()
                )
            }
            
            // 课程列表
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "正在加载课程信息...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.filteredCourses.isEmpty() && uiState.courses.isNotEmpty() -> {
                    EmptyFilterState(onClearFilters = viewModel::clearFilters)
                }
                uiState.courses.isEmpty() && uiState.errorMessage == null -> {
                    EmptyDataState(onRetry = { viewModel.loadStudentCourseInfo() })
                }
                else -> {
                    CourseInfoList(
                        courses = uiState.filteredCourses,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 可折叠搜索和筛选区域
 */
@Composable
private fun CollapsibleSearchAndFilterSection(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedYear: String,
    selectedSemester: String,
    selectedCourseType: String,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    onYearSelect: (String) -> Unit,
    onSemesterSelect: (String) -> Unit,
    onCourseTypeSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    hasActiveFilters: Boolean
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 搜索栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = onSearchChange,
                    placeholder = { Text("搜索课程名称或代码") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null)
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, "清除")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 筛选选项卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 学年筛选
                    FilterChipRow(
                        title = "学年",
                        options = listOf("") + availableYears,
                        selectedOption = selectedYear,
                        onSelect = onYearSelect,
                        displayText = { if (it.isEmpty()) "全部" else "${it}年" }
                    )
                    
                    // 学期筛选
                    FilterChipRow(
                        title = "学期",
                        options = listOf("", "1", "2"),
                        selectedOption = selectedSemester,
                        onSelect = onSemesterSelect,
                        displayText = { 
                            when (it) {
                                "" -> "全部"
                                "1" -> "第一学期"
                                "2" -> "第二学期"
                                else -> it
                            }
                        }
                    )
                    
                    // 课程类型筛选
                    if (availableCourseTypes.isNotEmpty()) {
                        FilterChipRow(
                            title = "课程类型",
                            options = listOf("") + availableCourseTypes,
                            selectedOption = selectedCourseType,
                            onSelect = onCourseTypeSelect,
                            displayText = { if (it.isEmpty()) "全部" else it }
                        )
                    }
                    
                    // 清除筛选按钮
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清除筛选")
                        }
                    }
                }
            }
        }
    }
    
    // 当筛选面板收起时，如果有活跃筛选，显示筛选摘要
    AnimatedVisibility(
        visible = !isExpanded && hasActiveFilters,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildString {
                        val filters = mutableListOf<String>()
                        if (selectedYear.isNotEmpty()) filters.add("${selectedYear}年")
                        if (selectedSemester.isNotEmpty()) filters.add(
                            when (selectedSemester) {
                                "1" -> "第一学期"
                                "2" -> "第二学期"
                                else -> selectedSemester
                            }
                        )
                        if (selectedCourseType.isNotEmpty()) filters.add(selectedCourseType)
                        if (searchKeyword.isNotEmpty()) filters.add("\"$searchKeyword\"")
                        append(filters.joinToString(" · "))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 搜索和筛选区域 (保留向后兼容)
 */
@Composable
private fun SearchAndFilterSection(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedYear: String,
    selectedSemester: String,
    selectedCourseType: String,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    onYearSelect: (String) -> Unit,
    onSemesterSelect: (String) -> Unit,
    onCourseTypeSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    hasActiveFilters: Boolean
) {
    var showFilters by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchChange,
                placeholder = { Text("搜索课程名称或代码") },
                leadingIcon = {
                    Icon(Icons.Default.Search, null)
                },
                trailingIcon = {
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 筛选按钮
            FilledTonalIconButton(
                onClick = { showFilters = !showFilters },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (hasActiveFilters) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    "筛选",
                    tint = if (hasActiveFilters)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 筛选面板
        AnimatedVisibility(
            visible = showFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 学年筛选
                    FilterChipRow(
                        title = "学年",
                        options = listOf("") + availableYears,
                        selectedOption = selectedYear,
                        onSelect = onYearSelect,
                        displayText = { if (it.isEmpty()) "全部" else "${it}年" }
                    )
                    
                    // 学期筛选
                    FilterChipRow(
                        title = "学期",
                        options = listOf("", "1", "2"),
                        selectedOption = selectedSemester,
                        onSelect = onSemesterSelect,
                        displayText = { 
                            when (it) {
                                "" -> "全部"
                                "1" -> "第一学期"
                                "2" -> "第二学期"
                                else -> it
                            }
                        }
                    )
                    
                    // 课程类型筛选
                    if (availableCourseTypes.isNotEmpty()) {
                        FilterChipRow(
                            title = "课程类型",
                            options = listOf("") + availableCourseTypes,
                            selectedOption = selectedCourseType,
                            onSelect = onCourseTypeSelect,
                            displayText = { if (it.isEmpty()) "全部" else it }
                        )
                    }
                    
                    // 清除筛选按钮
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清除筛选")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 筛选芯片行
 */
@Composable
private fun <T> FilterChipRow(
    title: String,
    options: List<T>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onSelect(option) },
                    label = { Text(displayText(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/**
 * 统计信息栏
 */
@Composable
private fun StatisticsBar(
    totalCourses: Int,
    totalCredits: Double
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "共 $totalCourses 门课程",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "共 $totalCredits 学分",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * 课程列表 - 支持平板多列布局
 */
@Composable
private fun CourseInfoList(
    courses: List<CourseInfoItem>,
    modifier: Modifier = Modifier
) {
    AdaptiveLayout(modifier) { config ->
        val columns = config.getListColumns()
        
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
            contentPadding = PaddingValues(config.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = courses.size,
                key = { "${courses[it].courseCode}_${courses[it].suggestedYear}_${courses[it].suggestedSemester}" }
            ) { index ->
                CourseInfoCard(course = courses[index])
            }
            
            // 底部留白
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * 课程信息卡片
 */
@Composable
private fun CourseInfoCard(course: CourseInfoItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 课程名称和学分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${course.credits}学分",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 课程代码
            Text(
                text = "课程代码: ${course.courseCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 课程详情标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 课程类型
                if (course.courseType.isNotEmpty()) {
                    InfoChip(
                        label = course.courseType,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                // 建议学期
                if (course.suggestedYear.isNotEmpty() || course.suggestedSemester.isNotEmpty()) {
                    val semesterText = when (course.suggestedSemester) {
                        "1" -> "第一学期"
                        "2" -> "第二学期"
                        else -> "第${course.suggestedSemester}学期"
                    }
                    InfoChip(
                        label = "${course.suggestedYear}年$semesterText",
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // 学时
                if (course.hours > 0) {
                    InfoChip(
                        label = "${course.hours}学时",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 考核方式
            if (course.examMethod.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "考核方式: ${course.examMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 信息标签
 */
@Composable
private fun InfoChip(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 筛选无结果状态
 */
@Composable
private fun EmptyFilterState(onClearFilters: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "没有符合筛选条件的课程",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onClearFilters) {
                Text("清除筛选条件")
            }
        }
    }
}

/**
 * 无数据状态
 */
@Composable
private fun EmptyDataState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无课程信息",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请确保您已登录并绑定学号",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新加载")
            }
        }
    }
}
