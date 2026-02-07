package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.data.model.CollegeOption
import com.suseoaa.projectoaa.data.model.MajorOption
import com.suseoaa.projectoaa.data.model.StudyRequirementCategory
import com.suseoaa.projectoaa.data.model.StudyRequirementCourse
import com.suseoaa.projectoaa.presentation.teachingplan.StudyRequirementViewModel
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/**
 * 修读要求查询界面
 * 可以浏览任意专业、年级的培养计划
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRequirementScreen(
    onBack: () -> Unit,
    viewModel: StudyRequirementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 找到选中的学院和专业对象
    val selectedCollegeObj = uiState.colleges.find { it.code == uiState.selectedCollegeId }
    val selectedMajorObj = uiState.majors.find { it.code == uiState.selectedMajorId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修读要求查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 展开/折叠全部按钮 (仅在有数据时显示)
                    if (uiState.categories.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (uiState.expandedCategories.size == uiState.categories.size) {
                                    viewModel.collapseAllCategories()
                                } else {
                                    viewModel.expandAllCategories()
                                }
                            }
                        ) {
                            Icon(
                                if (uiState.expandedCategories.size == uiState.categories.size)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = if (uiState.expandedCategories.size == uiState.categories.size)
                                    "全部折叠" else "全部展开"
                            )
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

            if (isTablet) {
                // 平板布局：左侧筛选，右侧内容
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 左侧筛选面板 (固定宽度)
                    TabletFilterPanel(
                        grades = uiState.grades,
                        colleges = uiState.colleges,
                        majors = uiState.majors,
                        selectedGrade = uiState.selectedGrade,
                        selectedCollege = selectedCollegeObj,
                        selectedMajor = selectedMajorObj,
                        onGradeSelect = viewModel::selectGrade,
                        onCollegeSelect = { viewModel.selectCollege(it.code) },
                        onMajorSelect = { viewModel.selectMajor(it.code) },
                        onQuery = viewModel::queryStudyRequirements,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.width(320.dp)
                    )

                    // 右侧内容区域
                    Column(modifier = Modifier.weight(1f)) {
                        // 错误提示 - 使用 Toast
                        uiState.errorMessage?.let { error ->
                            LaunchedEffect(error) {
                                ToastManager.showToast(error)
                                viewModel.clearError()
                            }
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
                                    CircularProgressIndicator()
                                }
                            }

                            uiState.categories.isEmpty() && selectedMajorObj != null -> {
                                EmptyState(message = "暂无课程数据")
                            }

                            uiState.categories.isEmpty() -> {
                                EmptyState(message = "请选择年级、学院、专业后查询")
                            }

                            else -> {
                                CourseListByCategory(
                                    categories = uiState.categories,
                                    expandedCategories = uiState.expandedCategories,
                                    onToggleCategory = viewModel::toggleCategoryExpanded,
                                    modifier = Modifier.weight(1f)
                                )

                                TotalCreditsBar(
                                    totalCredits = uiState.categories.sumOf { it.totalCredits }
                                )
                            }
                        }
                    }
                }
            } else {
                // 手机布局：垂直排列
                Column(modifier = Modifier.fillMaxSize()) {
                    // 可折叠筛选区域
                    CollapsibleFilterSection(
                        isExpanded = uiState.isFilterExpanded,
                        onToggleExpand = { viewModel.toggleFilterExpanded() },
                        grades = uiState.grades,
                        colleges = uiState.colleges,
                        majors = uiState.majors,
                        selectedGrade = uiState.selectedGrade,
                        selectedCollege = selectedCollegeObj,
                        selectedMajor = selectedMajorObj,
                        onGradeSelect = viewModel::selectGrade,
                        onCollegeSelect = { viewModel.selectCollege(it.code) },
                        onMajorSelect = { viewModel.selectMajor(it.code) },
                        onQuery = viewModel::queryStudyRequirements,
                        isLoading = uiState.isLoading,
                        hasResult = uiState.categories.isNotEmpty()
                    )

                    // 错误提示 - 使用 Toast
                    uiState.errorMessage?.let { error ->
                        LaunchedEffect(error) {
                            ToastManager.showToast(error)
                            viewModel.clearError()
                        }
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
                                CircularProgressIndicator()
                            }
                        }

                        uiState.categories.isEmpty() && selectedMajorObj != null -> {
                            EmptyState(message = "暂无课程数据")
                        }

                        uiState.categories.isEmpty() -> {
                            EmptyState(message = "请选择年级、学院、专业后查询")
                        }

                        else -> {
                            CourseListByCategory(
                                categories = uiState.categories,
                                expandedCategories = uiState.expandedCategories,
                                onToggleCategory = viewModel::toggleCategoryExpanded,
                                modifier = Modifier.weight(1f)
                            )

                            TotalCreditsBar(
                                totalCredits = uiState.categories.sumOf { it.totalCredits }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可折叠筛选区域
 */
@Composable
private fun CollapsibleFilterSection(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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
    hasResult: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 折叠头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "查询条件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isExpanded && (selectedGrade.isNotEmpty() || selectedCollege != null)) {
                            Text(
                                text = buildString {
                                    if (selectedGrade.isNotEmpty()) append("${selectedGrade}级")
                                    if (selectedCollege != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selectedCollege.name)
                                    }
                                    if (selectedMajor != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selectedMajor.name)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 可折叠内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

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
                        Text(
                            text = "加载专业中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        }
    }
}

/**
 * 平板布局筛选面板
 */
@Composable
private fun TabletFilterPanel(
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "查询条件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

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
                Text(
                    text = "加载专业中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

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
}

/**
 * 筛选区域 (保留向后兼容)
 */
@Composable
private fun FilterSection(
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

/**
 * iOS 风格选项卡片选择器 - 支持泛型类型
 */
@Composable
private fun <T> ChipSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        LazyRow(
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
                    animationSpec = tween(200),
                    label = "chipBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "chipContent"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 0.dp else 1.dp,
                    animationSpec = tween(200),
                    label = "chipBorder"
                )

                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor,
                    modifier = Modifier
                        .then(
                            if (!isSelected) Modifier.border(
                                width = borderWidth,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            ) else Modifier
                        )
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

/**
 * 可搜索的选项选择器 - 用于学院/专业等大量选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SearchableChipSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String,
    placeholder: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isEmpty()) options
        else options.filter { displayText(it).contains(searchQuery, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        // 已选中的选项显示
        if (selectedOption != null) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayText(selectedOption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // 未选中时显示点击选择按钮
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的选项列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // 搜索框
                    if (options.size > 5) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, "清除")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // 选项网格
                    val maxHeight = if (filteredOptions.size > 6) 200.dp else Dp.Unspecified
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredOptions.take(20).forEach { option ->
                            val isSelected = option == selectedOption
                            Surface(
                                onClick = {
                                    onSelect(option)
                                    isExpanded = false
                                    searchQuery = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayText(option),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredOptions.size > 20) {
                            Text(
                                text = "还有 ${filteredOptions.size - 20} 个选项，请搜索缩小范围",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (filteredOptions.isEmpty()) {
                            Text(
                                text = "未找到匹配项",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 通用下拉选择器 (保留兼容性)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T?) -> String,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = displayText(selectedOption),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayText(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 按课程类型分类的列表
 */
@Composable
private fun CourseListByCategory(
    categories: List<StudyRequirementCategory>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.categoryName }) { category ->
            CourseCategoryCard(
                category = category,
                isExpanded = expandedCategories.contains(category.categoryName),
                onToggle = { onToggleCategory(category.categoryName) }
            )
        }
    }
}

/**
 * 课程分类卡片
 */
@Composable
private fun CourseCategoryCard(
    category: StudyRequirementCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 分类标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${category.courses.size}门课程 · ${category.totalCredits}学分",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    category.courses.forEachIndexed { index, course ->
                        CourseItem(course = course)
                        if (index < category.courses.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课程项
 */
@Composable
private fun CourseItem(course: StudyRequirementCourse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "课程代码: ${course.courseCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (course.hours > 0) {
                    Text(
                        text = "${course.hours}学时",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 建议修读学期
            if (course.suggestedYear.isNotEmpty() || course.suggestedSemester.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "建议学期: ${course.suggestedYear}年第${course.suggestedSemester}学期",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // 学分标签
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
}

/**
 * 总学分栏
 */
@Composable
private fun TotalCreditsBar(totalCredits: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "培养计划总学分",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$totalCredits 学分",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * 空状态
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
                imageVector = Icons.Default.Search,
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
