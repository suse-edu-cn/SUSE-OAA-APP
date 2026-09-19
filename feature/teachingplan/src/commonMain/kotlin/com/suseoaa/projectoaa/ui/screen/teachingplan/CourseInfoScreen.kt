package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.CollegeOption
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.MajorOption
import com.suseoaa.projectoaa.presentation.teachingplan.CourseInfoViewModel
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.theme.AppDimensions
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.presentation.teachingplan.CourseInfoUiState
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.CollapsibleFilterPanel
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.CourseInfoList
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.EmptyFilterState
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.EmptyState
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.StatisticsBar
import com.suseoaa.projectoaa.ui.screen.teachingplan.courseinfo.TabletFilterPanel

// 课程信息查询页：手机/平板两套布局。

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

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            ToastManager.showToast(error)
            viewModel.clearError()
        }
    }

    AdaptivePageScaffold(
        title = "课程信息查询",
        onBack = onBack,
        sharedTransitionKey = "courseInfo",
        compactPadding = 0.dp,
        tabletPadding = 0.dp,
        actions = {
            if (uiState.courses.isNotEmpty()) {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        },
        compactContent = { modifier ->
            CourseInfoCompactLayout(
                uiState = uiState,
                selectedCollegeObj = selectedCollegeObj,
                selectedMajorObj = selectedMajorObj,
                availableYears = availableYears,
                availableCourseTypes = availableCourseTypes,
                viewModel = viewModel,
                focusManager = focusManager,
                modifier = modifier
            )
        },
        tabletContent = { modifier ->
            CourseInfoTabletLayout(
                uiState = uiState,
                selectedCollegeObj = selectedCollegeObj,
                selectedMajorObj = selectedMajorObj,
                availableYears = availableYears,
                availableCourseTypes = availableCourseTypes,
                viewModel = viewModel,
                focusManager = focusManager,
                modifier = modifier
            )
        }
    )
}

// ============================================================================
// 布局组件
// ============================================================================

/**
 * 平板端布局 - 左侧统一筛选面板 + 右侧内容
 */
@Composable
private fun CourseInfoTabletLayout(
    uiState: CourseInfoUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    viewModel: CourseInfoViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.screenPaddingMedium, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.paneSpacing)
    ) {
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
            modifier = Modifier.width(AppDimensions.sidePanelWidth)
        )

        CourseInfoMainContent(
            uiState = uiState,
            selectedMajorObj = selectedMajorObj,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 手机端布局 - 可折叠统一筛选区域 + 内容
 */
@Composable
private fun CourseInfoCompactLayout(
    uiState: CourseInfoUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    availableYears: List<String>,
    availableCourseTypes: List<String>,
    viewModel: CourseInfoViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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

        CourseInfoMainContent(
            uiState = uiState,
            selectedMajorObj = selectedMajorObj,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CourseInfoMainContent(
    uiState: CourseInfoUiState,
    selectedMajorObj: MajorOption?,
    viewModel: CourseInfoViewModel,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.filteredCourses.isNotEmpty()) {
            StatisticsBar(
                totalCourses = uiState.filteredCourses.size,
                totalCredits = viewModel.getTotalCredits()
            )
        }

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
    uiState: CourseInfoUiState,
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
