package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.suseoaa.projectoaa.data.model.CollegeOption
import com.suseoaa.projectoaa.data.model.CourseInfoItem
import com.suseoaa.projectoaa.data.model.MajorOption
import com.suseoaa.projectoaa.presentation.teachingplan.CourseInfoViewModel
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/**
 * 课程信息查询界面
 * 支持查询任意学院、专业、年级的课程信息
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
    
    // 找到选中的学院和专业对象
    val selectedCollegeObj = uiState.colleges.find { it.code == uiState.selectedCollegeId }
    val selectedMajorObj = uiState.majors.find { it.code == uiState.selectedMajorId }
    
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
                    // 刷新按钮
                    if (uiState.courses.isNotEmpty()) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, "刷新")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        // 使用 BoxWithConstraints 检测屏幕宽度以适配平板
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTablet = maxWidth > 600.dp
            val horizontalPadding = if (isTablet) 24.dp else 0.dp
            
            // 错误提示 - 使用 Toast
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    ToastManager.showToast(error)
                    viewModel.clearError()
                }
            }
            
            if (isTablet) {
                // 平板布局：左侧统一筛选面板，右侧纯内容
                TabletLayout(
                    uiState = uiState,
                    selectedCollegeObj = selectedCollegeObj,
                    selectedMajorObj = selectedMajorObj,
                    availableYears = availableYears,
                    availableCourseTypes = availableCourseTypes,
                    viewModel = viewModel,
                    focusManager = focusManager,
                    horizontalPadding = horizontalPadding
                )
            } else {
                // 手机布局：可折叠统一筛选区域
                PhoneLayout(
                    uiState = uiState,
                    selectedCollegeObj = selectedCollegeObj,
                    selectedMajorObj = selectedMajorObj,
                    availableYears = availableYears,
                    availableCourseTypes = availableCourseTypes,
                    viewModel = viewModel,
                    focusManager = focusManager
                )
            }
        }
    }
}

// ============================================================================
// 布局组件
// ============================================================================

/**
 * 平板端布局 - 左侧统一筛选面板 + 右侧内容
 */
@Composable
private fun TabletLayout(
    uiState: com.suseoaa.projectoaa.data.model.CourseInfoUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    viewModel: CourseInfoViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左侧统一筛选面板
        TabletFilterPanel(
            // 查询条件
            grades = uiState.grades,
            colleges = uiState.colleges,
            majors = uiState.majors,
            selectedGrade = uiState.selectedGrade,
            selectedCollege = selectedCollegeObj,
            selectedMajor = selectedMajorObj,
            onGradeSelect = viewModel::selectGrade,
            onCollegeSelect = { viewModel.selectCollege(it.code) },
            onMajorSelect = { viewModel.selectMajor(it.code) },
            onQuery = viewModel::queryCourses,
            isLoading = uiState.isLoading,
            // 课程筛选（有课程时显示）
            hasCourses = uiState.courses.isNotEmpty(),
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
            modifier = Modifier.width(320.dp)
        )
        
        // 右侧纯内容区域
        Column(modifier = Modifier.weight(1f)) {
            // 统计信息
            if (uiState.filteredCourses.isNotEmpty()) {
                StatisticsBar(
                    totalCourses = uiState.filteredCourses.size,
                    totalCredits = viewModel.getTotalCredits()
                )
            }
            
            // 课程列表
            CourseContentArea(
                uiState = uiState,
                selectedMajorObj = selectedMajorObj,
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 手机端布局 - 可折叠统一筛选区域 + 内容
 */
@Composable
private fun PhoneLayout(
    uiState: com.suseoaa.projectoaa.data.model.CourseInfoUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    viewModel: CourseInfoViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 可折叠统一筛选区域
        CollapsibleFilterPanel(
            isExpanded = uiState.isFilterExpanded,
            onToggleExpand = { viewModel.toggleFilterExpanded() },
            // 查询条件
            grades = uiState.grades,
            colleges = uiState.colleges,
            majors = uiState.majors,
            selectedGrade = uiState.selectedGrade,
            selectedCollege = selectedCollegeObj,
            selectedMajor = selectedMajorObj,
            onGradeSelect = viewModel::selectGrade,
            onCollegeSelect = { viewModel.selectCollege(it.code) },
            onMajorSelect = { viewModel.selectMajor(it.code) },
            onQuery = viewModel::queryCourses,
            isLoading = uiState.isLoading,
            // 课程筛选
            hasCourses = uiState.courses.isNotEmpty(),
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
            onClearFilters = viewModel::clearFilters
        )
        
        // 统计信息
        if (uiState.filteredCourses.isNotEmpty()) {
            StatisticsBar(
                totalCourses = uiState.filteredCourses.size,
                totalCredits = viewModel.getTotalCredits()
            )
        }
        
        // 课程列表
        CourseContentArea(
            uiState = uiState,
            selectedMajorObj = selectedMajorObj,
            onClearFilters = viewModel::clearFilters,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 课程内容区域 - 共享组件
 */
@Composable
private fun CourseContentArea(
    uiState: com.suseoaa.projectoaa.data.model.CourseInfoUiState,
    selectedMajorObj: MajorOption?,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.courses.isEmpty() && selectedMajorObj != null -> {
            EmptyState(message = "暂无课程数据")
        }
        uiState.courses.isEmpty() -> {
            EmptyState(message = "请选择年级、学院、专业后查询")
        }
        uiState.filteredCourses.isEmpty() -> {
            EmptyFilterState(onClearFilters = onClearFilters)
        }
        else -> {
            CourseInfoList(
                courses = uiState.filteredCourses,
                modifier = modifier
            )
        }
    }
}

// ============================================================================
// 筛选面板组件
// ============================================================================

/**
 * 手机端可折叠统一筛选面板
 * 将查询条件和课程筛选合并到一个可折叠区域
 */
@Composable
private fun CollapsibleFilterPanel(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    // 查询条件
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean,
    // 课程筛选
    hasCourses: Boolean,
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
    onClearFilters: () -> Unit
) {
    val hasActiveFilters = selectedYear.isNotEmpty() || 
                          selectedSemester.isNotEmpty() || 
                          selectedCourseType.isNotEmpty() ||
                          searchKeyword.isNotEmpty()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 筛选面板头部（始终可见，点击可折叠/展开）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            color = if (isExpanded) 
                MaterialTheme.colorScheme.surface 
            else if (hasActiveFilters || hasCourses)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildFilterSummaryText(
                            isExpanded = isExpanded,
                            selectedGrade = selectedGrade,
                            selectedCollege = selectedCollege,
                            selectedMajor = selectedMajor,
                            hasActiveFilters = hasActiveFilters,
                            searchKeyword = searchKeyword,
                            selectedYear = selectedYear,
                            selectedSemester = selectedSemester,
                            selectedCourseType = selectedCourseType
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 筛选状态指示
                if (!isExpanded && hasActiveFilters) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "已筛选",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // 可折叠内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FilterPanelContent(
                // 查询条件
                grades = grades,
                colleges = colleges,
                majors = majors,
                selectedGrade = selectedGrade,
                selectedCollege = selectedCollege,
                selectedMajor = selectedMajor,
                onGradeSelect = onGradeSelect,
                onCollegeSelect = onCollegeSelect,
                onMajorSelect = onMajorSelect,
                onQuery = onQuery,
                isLoading = isLoading,
                // 课程筛选
                hasCourses = hasCourses,
                searchKeyword = searchKeyword,
                onSearchChange = onSearchChange,
                onSearch = onSearch,
                selectedYear = selectedYear,
                selectedSemester = selectedSemester,
                selectedCourseType = selectedCourseType,
                availableYears = availableYears,
                availableCourseTypes = availableCourseTypes,
                onYearSelect = onYearSelect,
                onSemesterSelect = onSemesterSelect,
                onCourseTypeSelect = onCourseTypeSelect,
                onClearFilters = onClearFilters,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/**
 * 平板端统一筛选面板
 * 将查询条件和课程筛选放在同一个左侧面板
 */
@Composable
private fun TabletFilterPanel(
    // 查询条件
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean,
    // 课程筛选
    hasCourses: Boolean,
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            item {
                Text(
                    text = "查询与筛选",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
            
            // 查询条件区域
            item {
                QueryConditionSection(
                    grades = grades,
                    colleges = colleges,
                    majors = majors,
                    selectedGrade = selectedGrade,
                    selectedCollege = selectedCollege,
                    selectedMajor = selectedMajor,
                    onGradeSelect = onGradeSelect,
                    onCollegeSelect = onCollegeSelect,
                    onMajorSelect = onMajorSelect,
                    onQuery = onQuery,
                    isLoading = isLoading
                )
            }
            
            // 课程筛选区域（仅在有课程时显示）
            if (hasCourses) {
                item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                
                item {
                    CourseFilterContent(
                        searchKeyword = searchKeyword,
                        onSearchChange = onSearchChange,
                        onSearch = onSearch,
                        selectedYear = selectedYear,
                        selectedSemester = selectedSemester,
                        selectedCourseType = selectedCourseType,
                        availableYears = availableYears,
                        availableCourseTypes = availableCourseTypes,
                        onYearSelect = onYearSelect,
                        onSemesterSelect = onSemesterSelect,
                        onCourseTypeSelect = onCourseTypeSelect,
                        onClearFilters = onClearFilters
                    )
                }
            }
        }
    }
}

// ============================================================================
// 筛选面板子组件
// ============================================================================

/**
 * 筛选面板内容 - 手机端使用
 */
@Composable
private fun FilterPanelContent(
    // 查询条件
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean,
    // 课程筛选
    hasCourses: Boolean,
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        // 使用 LazyColumn 支持滚动，解决内容过多无法点击的问题
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 查询条件区域
            item {
                QueryConditionSection(
                    grades = grades,
                    colleges = colleges,
                    majors = majors,
                    selectedGrade = selectedGrade,
                    selectedCollege = selectedCollege,
                    selectedMajor = selectedMajor,
                    onGradeSelect = onGradeSelect,
                    onCollegeSelect = onCollegeSelect,
                    onMajorSelect = onMajorSelect,
                    onQuery = onQuery,
                    isLoading = isLoading
                )
            }
            
            // 课程筛选区域（仅在有课程时显示）
            if (hasCourses) {
                item {
                    CourseFilterContent(
                    searchKeyword = searchKeyword,
                    onSearchChange = onSearchChange,
                    onSearch = onSearch,
                    selectedYear = selectedYear,
                    selectedSemester = selectedSemester,
                    selectedCourseType = selectedCourseType,
                    availableYears = availableYears,
                    availableCourseTypes = availableCourseTypes,
                    onYearSelect = onYearSelect,
                    onSemesterSelect = onSemesterSelect,
                    onCourseTypeSelect = onCourseTypeSelect,
                        onClearFilters = onClearFilters
                    )
                }
            }
        }
    }
}

/**
 * 查询条件区域
 */
@Composable
private fun QueryConditionSection(
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 区域标题
        Text(
            text = "查询条件",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 年级选择
        ChipSelector(
            label = "入学年份",
            options = grades,
            selectedOption = selectedGrade,
            onSelect = onGradeSelect,
            displayText = { grade -> if (grade.isNotEmpty()) "${grade}级" else "" }
        )
        
        // 学院选择
        ChipSelector(
            label = "学院",
            options = colleges,
            selectedOption = selectedCollege,
            onSelect = onCollegeSelect,
            displayText = { it.name }
        )
        
        // 专业选择
        if (majors.isNotEmpty()) {
            ChipSelector(
                label = "专业",
                options = majors,
                selectedOption = selectedMajor,
                onSelect = onMajorSelect,
                displayText = { it.name }
            )
        } else if (selectedCollege != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "加载专业中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 查询按钮
        Button(
            onClick = onQuery,
            enabled = selectedGrade.isNotEmpty() && 
                     selectedCollege != null && 
                     selectedMajor != null && 
                     !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Search, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("查询")
            }
        }
    }
}

/**
 * 课程筛选内容区域
 */
@Composable
private fun CourseFilterContent(
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
    onClearFilters: () -> Unit
) {
    val hasActiveFilters = selectedYear.isNotEmpty() || 
                          selectedSemester.isNotEmpty() || 
                          selectedCourseType.isNotEmpty() ||
                          searchKeyword.isNotEmpty()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 搜索栏
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onSearchChange,
            placeholder = { Text("搜索课程名称或代码") },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchKeyword.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, "清除", modifier = Modifier.size(20.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        
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
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("清除筛选", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ============================================================================
// 辅助函数
// ============================================================================

/**
 * 构建筛选摘要文本
 */
private fun buildFilterSummaryText(
    isExpanded: Boolean,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    hasActiveFilters: Boolean,
    searchKeyword: String,
    selectedYear: String,
    selectedSemester: String,
    selectedCourseType: String
): String {
    if (isExpanded) return "查询与筛选"
    
    return buildString {
        // 查询条件摘要
        if (selectedGrade.isNotEmpty()) append("${selectedGrade}级")
        if (selectedCollege != null) {
            if (isNotEmpty()) append(" · ")
            append(selectedCollege.name)
        }
        if (selectedMajor != null) {
            if (isNotEmpty()) append(" · ")
            append(selectedMajor.name)
        }
        
        // 筛选条件摘要
        if (hasActiveFilters) {
            if (isNotEmpty()) append(" | ")
            val filters = mutableListOf<String>()
            if (searchKeyword.isNotEmpty()) filters.add("\"$searchKeyword\"")
            if (selectedYear.isNotEmpty()) filters.add("${selectedYear}年")
            if (selectedSemester.isNotEmpty()) {
                filters.add(when (selectedSemester) {
                    "1" -> "第一学期"
                    "2" -> "第二学期"
                    else -> selectedSemester
                })
            }
            if (selectedCourseType.isNotEmpty()) filters.add(selectedCourseType)
            append(filters.joinToString("·"))
        }
        
        if (isEmpty()) append("请选择查询条件")
    }
}

// ============================================================================
// 通用UI组件
// ============================================================================

/**
 * iOS 风格选项卡片选择器 - 支持泛型类型
 * 自动将选中项滚动到可见区域中间
 */
@Composable
private fun <T> ChipSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    val listState = rememberLazyListState()
    
    // 当选中项改变时，自动滚动到该项使其居中
    LaunchedEffect(selectedOption) {
        selectedOption?.let { selected ->
            val selectedIndex = options.indexOf(selected)
            if (selectedIndex >= 0) {
                // 滚动到选中项，尽量让其居中显示
                listState.animateScrollToItem(
                    index = selectedIndex,
                    scrollOffset = -100 // 向左偏移一点以尽量居中
                )
            }
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(options.size) { index ->
                val option = options[index]
                val isSelected = option == selectedOption
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = androidx.compose.animation.core.tween(200),
                    label = "chipBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = androidx.compose.animation.core.tween(200),
                    label = "chipContent"
                )
                
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor
                ) {
                    Text(
                        text = displayText(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// 其他UI组件
// ============================================================================

/**
 * 筛选芯片行
 * 自动将选中项滚动到可见区域
 */
@Composable
private fun <T> FilterChipRow(
    title: String,
    options: List<T>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    val listState = rememberLazyListState()
    
    // 当选中项改变时，自动滚动到该项使其可见
    LaunchedEffect(selectedOption) {
        val selectedIndex = options.indexOf(selectedOption)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -100
            )
        }
    }
    
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            state = listState,
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
 * 简单空状态组件
 */
@Composable
private fun EmptyState(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
