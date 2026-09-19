package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentFilterOption
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentUiState
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentViewModel
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.pickImageForAvatar
import org.koin.compose.viewmodel.koinViewModel

// 招新换届页面：按屏幕宽度切换手机/平板布局。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecruitmentScreen(
    onBack: () -> Unit,
    viewModel: RecruitmentViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTimeDialog by remember { mutableStateOf(false) }
    var launchAvatarPicker by remember { mutableStateOf(false) }

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

    if (launchAvatarPicker) {
        pickImageForAvatar { imageBytes ->
            viewModel.onAvatarPicked(imageBytes)
            launchAvatarPicker = false
        }
    }

    if (showTimeDialog) {
        TimeEditDialog(
            currentStart = uiState.startTime,
            currentEnd = uiState.endTime,
            onDismiss = { showTimeDialog = false },
            onConfirm = { start, end ->
                viewModel.updateTime(start, end)
                showTimeDialog = false
            }
        )
    }

    AdaptivePageScaffold(
        sharedTransitionKey = "recruitment_feature",
        title = "招新换届",
        onBack = onBack,
        actions = {
            if (uiState.canManageTime) {
                IconButton(onClick = { showTimeDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "修改填写时间"
                    )
                }
            }
        },
        compactContent = { modifier ->
            RecruitmentContent(
                modifier = modifier,
                uiState = uiState,
                isCompact = true,
                onPickAvatar = { launchAvatarPicker = true },
                onUpdateField = viewModel::updateFormField,
                onSubmit = viewModel::submitApplication,
                onFilterChange = viewModel::setFilterOption,
                onStatusChange = viewModel::changeSingleStatus
            )
        },
        tabletContent = { modifier ->
            RecruitmentContent(
                modifier = modifier,
                uiState = uiState,
                isCompact = false,
                onPickAvatar = { launchAvatarPicker = true },
                onUpdateField = viewModel::updateFormField,
                onSubmit = viewModel::submitApplication,
                onFilterChange = viewModel::setFilterOption,
                onStatusChange = viewModel::changeSingleStatus
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecruitmentContent(
    modifier: Modifier,
    uiState: RecruitmentUiState,
    isCompact: Boolean,
    onPickAvatar: () -> Unit,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit,
    onSubmit: () -> Unit,
    onFilterChange: (RecruitmentFilterOption) -> Unit,
    onStatusChange: (RecruitmentApplication, String) -> Unit
) {
    if (uiState.isLoading && uiState.applications.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecruitmentHeaderCard(uiState = uiState)

        if (uiState.isLoading && uiState.applications.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (uiState.isSubmissionTime) {
                if (isCompact) {
                    SubmissionForm(
                        application = uiState.currentApplication,
                        isCompact = true,
                        hasExistingApplication = uiState.applications.isNotEmpty(),
                        pickedAvatar = uiState.pickedAvatar,
                        onPickAvatar = onPickAvatar,
                        onUpdateField = onUpdateField,
                        onSubmit = onSubmit
                    )
                } else {
                    TabletSubmissionForm(
                        application = uiState.currentApplication,
                        hasExistingApplication = uiState.applications.isNotEmpty(),
                        pickedAvatar = uiState.pickedAvatar,
                        onPickAvatar = onPickAvatar,
                        onUpdateField = onUpdateField,
                        onSubmit = onSubmit
                    )
                }
            } else {
                if (isCompact) {
                    ReviewSection(
                        uiState = uiState,
                        isCompact = true,
                        onFilterChange = onFilterChange,
                        onStatusChange = onStatusChange
                    )
                } else {
                    TabletReviewSection(
                        uiState = uiState,
                        onFilterChange = onFilterChange,
                        onStatusChange = onStatusChange
                    )
                }
            }
        }
    }

}

@Composable
private fun RecruitmentHeaderCard(uiState: RecruitmentUiState) {
    val title = if (uiState.isSubmissionTime) "当前处于填写时间" else "当前不在填写时间"
    val subtitle = if (uiState.isSubmissionTime) {
        "仅可提交或修改你自己的申请表"
    } else {
        "当前阶段可查看申请并执行录取操作"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "填写时间：${formatRange(uiState.startTime, uiState.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("部门：${uiState.userDepartment.ifBlank { "未知" }}") }
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("职位：${uiState.userRole.ifBlank { "未知" }}") }
                )
            }

            if (!uiState.canManageTime) {
                Text(
                    text = "时间修改权限仅限：副部长、部长、会长、开发者",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
