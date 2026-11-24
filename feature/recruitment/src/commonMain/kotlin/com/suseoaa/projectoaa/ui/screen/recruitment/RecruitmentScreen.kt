package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentReviewViewModel
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentViewModel
import com.suseoaa.projectoaa.presentation.recruitment.TermManagementViewModel
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/** 招新换届页的 Tab，按权限决定显示哪些。 */
private enum class RecruitmentTab(val title: String) {
    Apply("我的申请"),
    Review("面试审核"),
    Terms("周期管理")
}

/**
 * 招新换届：在校成员可按周期提交申请；部门负责人及以上多一个面试审核，
 * 管理员再多一个周期与面试官管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecruitmentScreen(
    onBack: () -> Unit,
    viewModel: RecruitmentViewModel = koinViewModel(),
    reviewViewModel: RecruitmentReviewViewModel = koinViewModel(),
    termViewModel: TermManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviewState by reviewViewModel.uiState.collectAsState()
    val termState by termViewModel.uiState.collectAsState()

    val tabs = buildList {
        add(RecruitmentTab.Apply)
        if (uiState.canReview) add(RecruitmentTab.Review)
        if (uiState.canManageTerms) add(RecruitmentTab.Terms)
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedTab = tabs.getOrElse(selectedIndex) { RecruitmentTab.Apply }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            ToastManager.showError(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            ToastManager.showSuccess(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(reviewState.message) {
        reviewState.message?.let {
            ToastManager.showToast(it)
            reviewViewModel.clearMessage()
        }
    }
    LaunchedEffect(termState.message) {
        termState.message?.let {
            ToastManager.showToast(it)
            termViewModel.clearMessage()
        }
    }
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            RecruitmentTab.Review -> reviewViewModel.ensureLoaded()
            RecruitmentTab.Terms -> termViewModel.ensureLoaded()
            RecruitmentTab.Apply -> Unit
        }
    }

    AdaptivePageScaffold(
        sharedTransitionKey = "recruitment_feature",
        title = "招新换届",
        onBack = onBack
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            if (tabs.size > 1) {
                PrimaryTabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = tab == selectedTab
                        Tab(
                            selected = selected,
                            onClick = { selectedIndex = index },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(tab.title) }
                        )
                    }
                }
            }
            // 平板上限制内容宽度，表单拉满整屏不好读
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val contentModifier = Modifier.fillMaxWidth().widthIn(max = 840.dp)
                when (selectedTab) {
                    RecruitmentTab.Apply -> ApplicationTab(
                        uiState = uiState,
                        onRefresh = viewModel::loadInitialData,
                        onSelectTerm = viewModel::selectTerm,
                        onUpdateForm = viewModel::updateForm,
                        onUploadAvatar = viewModel::uploadAvatar,
                        onSelectDepartment = viewModel::selectDepartment,
                        onSelectRole = viewModel::selectRole,
                        onClearChoice = viewModel::clearChoice,
                        onSubmit = viewModel::submitApplication,
                        onWithdraw = viewModel::deleteApplication,
                        modifier = contentModifier
                    )
                    RecruitmentTab.Review -> ReviewTab(
                        uiState = reviewState,
                        onSelectTerm = reviewViewModel::selectTerm,
                        onSelectDepartment = reviewViewModel::selectDepartment,
                        onSelectHasResult = reviewViewModel::selectHasResult,
                        onUpdateNameFilter = reviewViewModel::updateNameFilter,
                        onSearchByName = reviewViewModel::searchByName,
                        onOpenExportDialog = reviewViewModel::loadExportFields,
                        onExportResults = reviewViewModel::exportResults,
                        onConsumeExportedFile = reviewViewModel::consumeExportedFile,
                        onReportMessage = reviewViewModel::reportMessage,
                        onRefresh = reviewViewModel::reload,
                        onSaveDecision = reviewViewModel::saveDecision,
                        modifier = contentModifier
                    )
                    RecruitmentTab.Terms -> TermManagementTab(
                        uiState = termState,
                        onUpdateFilters = termViewModel::updateFilters,
                        onRefresh = termViewModel::reload,
                        onSaveTerm = termViewModel::saveTerm,
                        onDeleteTerm = termViewModel::deleteTerm,
                        onOpenInterviewerManager = termViewModel::openInterviewerManager,
                        onSearchUsers = termViewModel::searchUsers,
                        onAddInterviewers = termViewModel::addInterviewers,
                        onUpdateInterviewerRemark = termViewModel::updateInterviewerRemark,
                        onRemoveInterviewer = termViewModel::removeInterviewer,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}
