package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentFilterOption
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentUiState
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentViewModel
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.pickImageForAvatar
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

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
        modifier = Modifier.sharedBoundsTransition("recruitment_feature"),
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
private fun TabletSubmissionForm(
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
private fun TabletReviewSection(
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
private fun TabletApplicationListItem(
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

@Composable
private fun SubmissionForm(
    application: RecruitmentApplication,
    isCompact: Boolean,
    hasExistingApplication: Boolean,
    pickedAvatar: ByteArray?,
    onPickAvatar: () -> Unit,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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

            if (isCompact) {
                ApplicationFieldsColumn(
                    application = application,
                    isEditable = true,
                    onUpdateField = onUpdateField
                )
            } else {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSection(
    uiState: RecruitmentUiState,
    isCompact: Boolean,
    onFilterChange: (RecruitmentFilterOption) -> Unit,
    onStatusChange: (RecruitmentApplication, String) -> Unit
) {
    val apps = uiState.filteredApplications
    val allApps = uiState.applications

    Card(
        modifier = Modifier.fillMaxSize(),
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
                    allCount = allApps.size,
                    firstChoiceCount = allApps.count { it.choice1 == uiState.userDepartment },
                    secondChoiceCount = allApps.count { it.choice2 == uiState.userDepartment },
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
                val pagerState = rememberPagerState(pageCount = { apps.size })
                val scope = rememberCoroutineScope()

                LaunchedEffect(apps.size) {
                    if (apps.isEmpty()) return@LaunchedEffect
                    val maxIndex = apps.lastIndex
                    if (pagerState.currentPage > maxIndex) {
                        pagerState.scrollToPage(maxIndex)
                    }
                }

                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 4.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    apps.forEachIndexed { index, item ->
                        val title = item.name.ifBlank { item.resolvedStudentId.ifBlank { "未命名" } }
                        Tab(
                            selected = index == pagerState.currentPage,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    ReviewApplicationCard(
                        application = apps[page],
                        isCompact = isCompact,
                        canReview = uiState.canReviewApplications,
                        currentDepartment = uiState.userDepartment,
                        onStatusChange = onStatusChange
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "上一份")
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${apps.size}",
                        style = MaterialTheme.typography.labelLarge
                    )

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < apps.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "下一份")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    activeFilter: RecruitmentFilterOption,
    currentDepartment: String,
    allCount: Int,
    firstChoiceCount: Int,
    secondChoiceCount: Int,
    onFilterChange: (RecruitmentFilterOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "筛选条件（当前部门：${currentDepartment.ifBlank { "未知" }}）",
            style = MaterialTheme.typography.titleSmall
        )

        FilterChip(
            selected = activeFilter == RecruitmentFilterOption.FirstChoiceCurrentDepartment,
            onClick = { onFilterChange(RecruitmentFilterOption.FirstChoiceCurrentDepartment) },
            label = { Text("第一志愿是当前部门 ($firstChoiceCount)") }
        )

        FilterChip(
            selected = activeFilter == RecruitmentFilterOption.SecondChoiceCurrentDepartment,
            onClick = { onFilterChange(RecruitmentFilterOption.SecondChoiceCurrentDepartment) },
            label = { Text("第二志愿是当前部门 ($secondChoiceCount)") }
        )

        FilterChip(
            selected = activeFilter == RecruitmentFilterOption.All,
            onClick = { onFilterChange(RecruitmentFilterOption.All) },
            label = { Text("全部 ($allCount)") }
        )
    }
}

@Composable
private fun ReviewApplicationCard(
    application: RecruitmentApplication,
    isCompact: Boolean,
    canReview: Boolean,
    currentDepartment: String,
    onStatusChange: (RecruitmentApplication, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarSection(
            avatarUrl = application.avatarUrl,
            pickedAvatar = null,
            isEditable = false,
            onPickAvatar = {}
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = application.name.ifBlank { "未填写姓名" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "学号：${application.resolvedStudentId.ifBlank { "未知" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "状态：${application.status.ifBlank { "待处理" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isCompact) {
            ApplicationFieldsColumn(
                application = application,
                isEditable = false,
                onUpdateField = {}
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApplicationLeftFields(
                        application = application,
                        isEditable = false,
                        onUpdateField = {}
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApplicationRightFields(
                        application = application,
                        isEditable = false,
                        onUpdateField = {}
                    )
                }
            }
        }

        AdjustmentSwitch(
            isChecked = application.adjustment == 1,
            enabled = false,
            onCheckedChange = {}
        )

        if (canReview) {
            HorizontalDivider()
            StatusActions(
                application = application,
                currentDepartment = currentDepartment,
                onStatusChange = onStatusChange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AvatarSection(
    avatarUrl: String,
    pickedAvatar: ByteArray?,
    isEditable: Boolean,
    onPickAvatar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    pickedAvatar != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(pickedAvatar),
                            contentDescription = "头像预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    avatarUrl.isNotBlank() -> {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEditable) "申请表头像" else "头像",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isEditable) {
                        "提交申请前请先上传头像"
                    } else {
                        if (avatarUrl.isBlank()) "未上传头像" else "已上传头像"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isEditable) {
                Button(onClick = onPickAvatar) {
                    Text(if (pickedAvatar == null) "上传头像" else "重新选择")
                }
            }
        }
    }
}

@Composable
private fun ApplicationFieldsColumn(
    application: RecruitmentApplication,
    isEditable: Boolean,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit
) {
    ApplicationLeftFields(
        application = application,
        isEditable = isEditable,
        onUpdateField = onUpdateField
    )
    ApplicationRightFields(
        application = application,
        isEditable = isEditable,
        onUpdateField = onUpdateField
    )
}

@Composable
private fun ApplicationLeftFields(
    application: RecruitmentApplication,
    isEditable: Boolean,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit
) {
    RecruitmentTextField("专业", application.major, isEditable) { value ->
        onUpdateField { it.copy(major = value) }
    }
    RecruitmentTextField("班级", application.className, isEditable) { value ->
        onUpdateField { it.copy(className = value) }
    }
    RecruitmentTextField("电话", application.phone, isEditable) { value ->
        onUpdateField { it.copy(phone = value) }
    }
    RecruitmentTextField("QQ", application.qq, isEditable) { value ->
        onUpdateField { it.copy(qq = value) }
    }
    RecruitmentTextField("性别", application.gender, isEditable) { value ->
        onUpdateField { it.copy(gender = value) }
    }
    RecruitmentTextField("生日", application.birthday, isEditable) { value ->
        onUpdateField { it.copy(birthday = value) }
    }
    RecruitmentTextField("政治面貌", application.politic_stance, isEditable) { value ->
        onUpdateField { it.copy(politic_stance = value) }
    }
}

@Composable
private fun ApplicationRightFields(
    application: RecruitmentApplication,
    isEditable: Boolean,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit
) {
    RecruitmentTextField("第一志愿部门", application.choice1, isEditable) { value ->
        onUpdateField { it.copy(choice1 = value) }
    }
    RecruitmentTextField("第二志愿部门", application.choice2, isEditable) { value ->
        onUpdateField { it.copy(choice2 = value) }
    }
    RecruitmentTextField("申请职位一", application.role1, isEditable) { value ->
        onUpdateField { it.copy(role1 = value) }
    }
    RecruitmentTextField("申请职位二", application.role2, isEditable) { value ->
        onUpdateField { it.copy(role2 = value) }
    }
    RecruitmentTextField(
        label = "工作经历",
        value = application.experience,
        isEditable = isEditable,
        minLines = 3,
        onValueChange = { value -> onUpdateField { it.copy(experience = value) } }
    )
    RecruitmentTextField(
        label = "申请理由",
        value = application.reason,
        isEditable = isEditable,
        minLines = 3,
        onValueChange = { value -> onUpdateField { it.copy(reason = value) } }
    )
}

@Composable
private fun RecruitmentTextField(
    label: String,
    value: String,
    isEditable: Boolean,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = !isEditable,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun AdjustmentSwitch(
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("是否服从调剂", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (isChecked) "当前：服从调剂（1）" else "当前：不服从调剂（0）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun StatusActions(
    application: RecruitmentApplication,
    currentDepartment: String,
    onStatusChange: (RecruitmentApplication, String) -> Unit
) {
    var adjustmentDepartment by rememberSaveable(application.resolvedStudentId) {
        mutableStateOf(currentDepartment.ifBlank { "秘书处" })
    }
    var adjustmentRole by rememberSaveable(application.resolvedStudentId) {
        mutableStateOf("副部长")
    }
    val adjustmentStatus = "调剂到${adjustmentDepartment.trim()}部门${adjustmentRole.trim()}职位"
    val adjustmentReady = adjustmentDepartment.isNotBlank() && adjustmentRole.isNotBlank()

    Text(
        text = "录取操作",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onStatusChange(application, "录取第1志愿") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(text = "录取第1志愿")
            }

            Button(
                onClick = { onStatusChange(application, "录取第2志愿") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text(text = "录取第2志愿")
            }
        }

        Button(
            onClick = { onStatusChange(application, "未通过") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            Text(text = "未通过")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "调剂录取（严格格式）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                RecruitmentTextField(
                    label = "调剂部门",
                    value = adjustmentDepartment,
                    isEditable = true,
                    onValueChange = { adjustmentDepartment = it }
                )

                RecruitmentTextField(
                    label = "调剂职位",
                    value = adjustmentRole,
                    isEditable = true,
                    onValueChange = { adjustmentRole = it }
                )

                Text(
                    text = "将提交为：$adjustmentStatus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onStatusChange(application, adjustmentStatus) },
                    enabled = adjustmentReady,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))
                ) {
                    Text(text = "按该格式调剂")
                }
            }
        }
    }
}

@Composable
private fun TimeEditDialog(
    currentStart: String,
    currentEnd: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var start by remember { mutableStateOf(currentStart) }
    var end by remember { mutableStateOf(currentEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改招新时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecruitmentTextField(
                    label = "开始时间 (yyyy-MM-dd HH:mm:ss)",
                    value = start,
                    isEditable = true,
                    onValueChange = { start = it }
                )
                RecruitmentTextField(
                    label = "结束时间 (yyyy-MM-dd HH:mm:ss)",
                    value = end,
                    isEditable = true,
                    onValueChange = { end = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(start, end) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatRange(start: String, end: String): String {
    if (start.isBlank() || end.isBlank()) return "暂未设置"
    return "$start 至 $end"
}
