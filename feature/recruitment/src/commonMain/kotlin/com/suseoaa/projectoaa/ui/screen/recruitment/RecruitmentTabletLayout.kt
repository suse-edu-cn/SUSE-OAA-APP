package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentFilterOption
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentUiState
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication

// 平板宽屏下的左右分栏布局。

@Composable
internal fun TabletSubmissionForm(
    application: RecruitmentApplication,
    hasExistingApplication: Boolean,
    pickedAvatar: ByteArray?,
    onPickAvatar: () -> Unit,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (hasExistingApplication) "修改我的申请表" else "填写申请表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                AvatarSection(
                    avatarUrl = application.avatarUrl,
                    pickedAvatar = pickedAvatar,
                    isEditable = true,
                    onPickAvatar = onPickAvatar
                )

                RecruitmentTextField(
                    label = "姓名（系统自动识别）",
                    value = application.name,
                    isEditable = false,
                    onValueChange = {}
                )

                AdjustmentSwitch(
                    isChecked = application.adjustment == 1,
                    enabled = true,
                    onCheckedChange = { checked ->
                        onUpdateField { it.copy(adjustment = if (checked) 1 else 0) }
                    }
                )

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (hasExistingApplication) "保存修改" else "提交申请",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ApplicationLeftFields(
                            application = application,
                            isEditable = true,
                            onUpdateField = onUpdateField
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ApplicationRightFields(
                            application = application,
                            isEditable = true,
                            onUpdateField = onUpdateField
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabletReviewSection(
    uiState: RecruitmentUiState,
    onFilterChange: (RecruitmentFilterOption) -> Unit,
    onStatusChange: (RecruitmentApplication, String) -> Unit
) {
    val apps = uiState.filteredApplications
    var selectedIndex by remember(apps) { mutableStateOf(0) }

    LaunchedEffect(apps.size) {
        if (selectedIndex > apps.lastIndex) {
            selectedIndex = 0
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.canReviewApplications) {
                    FilterBar(
                        activeFilter = uiState.activeFilter,
                        currentDepartment = uiState.userDepartment,
                        allCount = uiState.applications.size,
                        firstChoiceCount = uiState.applications.count { it.choice1 == uiState.userDepartment },
                        secondChoiceCount = uiState.applications.count { it.choice2 == uiState.userDepartment },
                        onFilterChange = onFilterChange
                    )
                }

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当前筛选下没有申请表",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "申请列表（${apps.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(apps) { index, application ->
                            TabletApplicationListItem(
                                application = application,
                                isSelected = index == selectedIndex,
                                onClick = { selectedIndex = index }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无可展示申请",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ReviewApplicationCard(
                    application = apps[selectedIndex],
                    isCompact = false,
                    canReview = uiState.canReviewApplications,
                    currentDepartment = uiState.userDepartment,
                    onStatusChange = onStatusChange
                )
            }
        }
    }
}

@Composable
internal fun TabletApplicationListItem(
    application: RecruitmentApplication,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val titleColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = application.name.ifBlank { application.resolvedStudentId.ifBlank { "未命名" } },
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "第一志愿：${application.choice1.ifBlank { "未填写" }}",
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.8f)
            )
            Text(
                text = "状态：${application.status.ifBlank { "待处理" }}",
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.8f)
            )
        }
    }
}
