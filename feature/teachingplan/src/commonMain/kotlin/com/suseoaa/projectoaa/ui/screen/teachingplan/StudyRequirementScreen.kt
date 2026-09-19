package com.suseoaa.projectoaa.ui.screen.teachingplan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.CollegeOption
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.MajorOption
import com.suseoaa.projectoaa.presentation.teachingplan.StudyRequirementViewModel
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.theme.AppDimensions
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.presentation.teachingplan.StudyRequirementUiState
import com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement.CollapsibleFilterSection
import com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement.CourseListByCategory
import com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement.EmptyState
import com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement.TabletFilterPanel
import com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement.TotalCreditsBar

// 修读要求页：手机/平板两套布局。

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

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            ToastManager.showToast(error)
            viewModel.clearError()
        }
    }

    AdaptivePageScaffold(
        title = "修读要求查询",
        onBack = onBack,
        sharedTransitionKey = "studyRequirement",
        compactPadding = 0.dp,
        tabletPadding = 0.dp,
        actions = {
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
        compactContent = { modifier ->
            StudyRequirementCompactLayout(
                uiState = uiState,
                selectedCollegeObj = selectedCollegeObj,
                selectedMajorObj = selectedMajorObj,
                viewModel = viewModel,
                modifier = modifier
            )
        },
        tabletContent = { modifier ->
            StudyRequirementTabletLayout(
                uiState = uiState,
                selectedCollegeObj = selectedCollegeObj,
                selectedMajorObj = selectedMajorObj,
                viewModel = viewModel,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun StudyRequirementTabletLayout(
    uiState: StudyRequirementUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    viewModel: StudyRequirementViewModel,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.screenPaddingMedium, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.paneSpacing)
    ) {
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
            modifier = Modifier.width(AppDimensions.sidePanelWidth)
        )

        StudyRequirementContent(
            uiState = uiState,
            selectedMajorObj = selectedMajorObj,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StudyRequirementCompactLayout(
    uiState: StudyRequirementUiState,
    selectedCollegeObj: CollegeOption?,
    selectedMajorObj: MajorOption?,
    viewModel: StudyRequirementViewModel,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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

        StudyRequirementContent(
            uiState = uiState,
            selectedMajorObj = selectedMajorObj,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StudyRequirementContent(
    uiState: StudyRequirementUiState,
    selectedMajorObj: MajorOption?,
    viewModel: StudyRequirementViewModel,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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
