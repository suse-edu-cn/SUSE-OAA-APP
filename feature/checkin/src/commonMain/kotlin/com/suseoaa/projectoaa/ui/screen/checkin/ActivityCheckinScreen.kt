package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.checkin.ActivityCheckinViewModel
import com.suseoaa.projectoaa.presentation.checkin.ActivityCheckinUiState
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldCheckinTask
import com.suseoaa.projectoaa.shared.database.NearFieldTask
import com.suseoaa.projectoaa.util.PlatformPermissionManager
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.AppPredictiveBackHandler
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// 近场活动签到主页：发现与历史两个视图的编排。

/**
 * 近场活动签到界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCheckinScreen(
    onBack: () -> Unit,
    viewModel: ActivityCheckinViewModel = koinViewModel(),
    permissionManager: PlatformPermissionManager = remember { PlatformPermissionManager() },
    toastManager: ToastManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showHostDialog by remember { mutableStateOf(false) }
    var isRequestingPermission by remember { mutableStateOf(false) }
    var isRequestingHardware by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedTaskForParticipants by remember { mutableStateOf<String?>(null) }

    // 对话框状态
    var showCheckinConfirmDialog by remember { mutableStateOf<NearFieldCheckinTask?>(null) }
    var showManualAddDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteTaskDialog by remember { mutableStateOf<String?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showRestartHostDialog by remember { mutableStateOf<NearFieldTask?>(null) }

    // 挂起的动作类型
    var pendingAction by remember { mutableStateOf<PendingCheckinAction?>(null) }
    var pendingHostParams by remember { mutableStateOf<Pair<String, String>?>(null) }

    // 处理回退逻辑
    AppPredictiveBackHandler(
        enabled = uiState.isBroadcasting,
        onProgress = {},
        onBack = { showExitConfirmDialog = true }
    )

    val handleBack = {
        if (selectedTaskForParticipants != null) {
            selectedTaskForParticipants = null
        } else if (uiState.isBroadcasting) {
            showExitConfirmDialog = true
        } else {
            onBack()
        }
    }

    // 处理错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastManager.showToast(it)
        }
    }

    // 进入功能时自动检查权限并开启扫描
    LaunchedEffect(Unit) {
        if (permissionManager.hasNearFieldPermissions()) {
            if (permissionManager.isHardwareEnabled()) {
                viewModel.startScanning()
            } else {
                pendingAction = PendingCheckinAction.SCAN
                isRequestingHardware = true
            }
        } else {
            pendingAction = PendingCheckinAction.SCAN
            isRequestingPermission = true
        }
    }

    // 权限与硬件请求逻辑
    if (isRequestingPermission) {
        permissionManager.RequestNearFieldPermissions { granted ->
            isRequestingPermission = false
            if (granted) {
                if (permissionManager.isHardwareEnabled()) {
                    executePendingAction(pendingAction, viewModel, pendingHostParams) {
                        pendingAction = null
                        pendingHostParams = null
                    }
                } else {
                    isRequestingHardware = true
                }
            } else {
                toastManager.showToast("需要相关权限才能使用此功能")
                pendingAction = null
                pendingHostParams = null
            }
        }
    }

    if (isRequestingHardware) {
        permissionManager.RequestEnableHardware { enabled ->
            isRequestingHardware = false
            if (enabled) {
                executePendingAction(pendingAction, viewModel, pendingHostParams) {
                    pendingAction = null
                    pendingHostParams = null
                }
            } else {
                toastManager.showToast("请开启Wi-Fi或蓝牙以使用此功能")
                pendingAction = null
                pendingHostParams = null
            }
        }
    }

    // 页面布局
    AdaptivePageScaffold(
        sharedTransitionKey = "activity_checkin_feature",
        title = "活动签到",
        onBack = handleBack,
        actions = {
            IconButton(onClick = { showHostDialog = true }) {
                Icon(
                    if (uiState.isBroadcasting) Icons.Default.CastConnected else Icons.Default.Cast,
                    contentDescription = "发布任务",
                    tint = if (uiState.isBroadcasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        compactContent = { modifier ->
            ActivityCheckinContent(
                modifier = modifier,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                uiState = uiState,
                viewModel = viewModel,
                permissionManager = permissionManager,
                onCheckinClick = { showCheckinConfirmDialog = it },
                onRequestPermissions = {
                    isRequestingPermission = true
                    isRequestingHardware = true
                },
                onTaskClick = { taskId ->
                    viewModel.loadParticipants(taskId)
                    selectedTaskForParticipants = taskId
                },
                onDeleteClick = { showDeleteTaskDialog = it },
                onRestartClick = { showRestartHostDialog = it },
                selectedTaskForParticipants = selectedTaskForParticipants,
                onDismissParticipants = { selectedTaskForParticipants = null },
                onManualAdd = { showManualAddDialog = selectedTaskForParticipants },
                onSync = { viewModel.syncParticipants(selectedTaskForParticipants!!) },
                onDeleteParticipant = { id ->
                    viewModel.deleteParticipant(
                        id,
                        selectedTaskForParticipants!!
                    )
                }
            )
        },
        tabletContent = { modifier ->
            ActivityCheckinContent(
                modifier = modifier,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                uiState = uiState,
                viewModel = viewModel,
                permissionManager = permissionManager,
                onCheckinClick = { showCheckinConfirmDialog = it },
                onRequestPermissions = {
                    isRequestingPermission = true
                    isRequestingHardware = true
                },
                onTaskClick = { taskId ->
                    viewModel.loadParticipants(taskId)
                    selectedTaskForParticipants = taskId
                },
                onDeleteClick = { showDeleteTaskDialog = it },
                onRestartClick = { showRestartHostDialog = it },
                selectedTaskForParticipants = selectedTaskForParticipants,
                onDismissParticipants = { selectedTaskForParticipants = null },
                onManualAdd = { showManualAddDialog = selectedTaskForParticipants },
                onSync = { viewModel.syncParticipants(selectedTaskForParticipants!!) },
                onDeleteParticipant = { id ->
                    viewModel.deleteParticipant(
                        id,
                        selectedTaskForParticipants!!
                    )
                }
            )
        }
    )

    // --- 各类对话框 ---

    // 1. 签到确认与姓名填写
    showCheckinConfirmDialog?.let { task ->
        var currentName by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            currentName = viewModel.getCurrentPerson()?.name ?: ""
        }

        AlertDialog(
            onDismissRequest = { showCheckinConfirmDialog = null },
            title = { Text("确认签到信息") },
            text = {
                Column {
                    Text(
                        "签到活动: ${task.activityName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = { currentName = it },
                        label = { Text("您的姓名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performCheckin(task, currentName)
                        showCheckinConfirmDialog = null
                    },
                    enabled = currentName.isNotBlank()
                ) { Text("确认签到") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckinConfirmDialog = null }) { Text("取消") }
            }
        )
    }

    // 2. 手动补签对话框
    showManualAddDialog?.let { taskId ->
        var name by remember { mutableStateOf("") }
        var studentId by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("补签") }
        var customReason by remember { mutableStateOf("") }
        val statusOptions = listOf("正常", "迟到", "补签", "自定义")

        AlertDialog(
            onDismissRequest = { showManualAddDialog = null },
            title = { Text("手动补签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("姓名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("学号") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("签到状态", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.forEach { opt ->
                            FilterChip(
                                selected = status == opt,
                                onClick = { status = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                    if (status == "自定义") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            label = { Text("自定义原因") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("请输入原因，如：请假、设备故障等") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalStatus =
                            if (status == "自定义") customReason.ifBlank { "自定义" } else status
                        viewModel.manualAddParticipant(taskId, name, studentId, finalStatus)
                        showManualAddDialog = null
                        viewModel.loadParticipants(taskId)
                    },
                    enabled = name.isNotBlank() && studentId.isNotBlank() && (status != "自定义" || customReason.isNotBlank())
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = null }) { Text("取消") }
            }
        )
    }

    // 3. 删除任务确认
    showDeleteTaskDialog?.let { taskId ->
        AlertDialog(
            onDismissRequest = { showDeleteTaskDialog = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除该签到任务及其所有签到记录吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(taskId)
                        showDeleteTaskDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTaskDialog = null }) { Text("取消") }
            }
        )
    }

    // 4. 退出确认对话框
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("停止签到？") },
            text = { Text("退出页面将停止近场广播，其他同学将无法再搜索到并进行签到。确定要退出吗？") },
            confirmButton = {
                Button(onClick = {
                    showExitConfirmDialog = false
                    viewModel.stopHosting()
                    onBack()
                }) { Text("确定退出") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    // 5. 重新开启任务对话框
    showRestartHostDialog?.let { task ->
        HostTaskDialog(
            isBroadcasting = false,
            initialActivityName = task.activityName,
            initialHostName = task.hostName,
            onStart = { activityName, hostName, startTime, endTime ->
                viewModel.startHosting(
                    activityName = activityName,
                    hostName = hostName,
                    startTime = startTime,
                    endTime = endTime,
                    existingTaskIdentifier = task.taskIdentifier,
                    securityNonce = task.securityNonce
                )
                showRestartHostDialog = null
            },
            onStop = {},
            onDismiss = { showRestartHostDialog = null }
        )
    }

    // 6. 主机发布对话框
    if (showHostDialog) {
        HostTaskDialog(
            isBroadcasting = uiState.isBroadcasting,
            onStart = { activityName, hostName, startTime, endTime ->
                if (permissionManager.hasNearFieldPermissions()) {
                    if (permissionManager.isHardwareEnabled()) {
                        viewModel.startHosting(activityName, hostName, startTime, endTime)
                        showHostDialog = false
                    } else {
                        pendingAction = PendingCheckinAction.HOST
                        pendingHostParams = activityName to hostName
                        isRequestingHardware = true
                        showHostDialog = false
                    }
                } else {
                    pendingAction = PendingCheckinAction.HOST
                    pendingHostParams = activityName to hostName
                    isRequestingPermission = true
                    showHostDialog = false
                }
            },
            onStop = {
                viewModel.stopHosting()
                showHostDialog = false
            },
            onDismiss = { showHostDialog = false }
        )
    }
}

/**
 * 挂起的动作类型
 */
enum class PendingCheckinAction {
    SCAN, HOST
}

/**
 * 执行挂起的动作
 */
internal fun executePendingAction(
    action: PendingCheckinAction?,
    viewModel: ActivityCheckinViewModel,
    hostParams: Pair<String, String>?,
    onClearPending: () -> Unit
) {
    when (action) {
        PendingCheckinAction.SCAN -> {
            viewModel.startScanning()
        }

        PendingCheckinAction.HOST -> {
            hostParams?.let { (activity, host) ->
                val now = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
                viewModel.startHosting(activity, host, now, now + 30 * 60 * 1000)
            }
        }

        null -> {}
    }
    onClearPending()
}

internal fun formatTimestamp(timestamp: Long): String {
    // 跨平台简易时间格式化
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"
}

@Composable
fun ActivityCheckinContent(
    modifier: Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    uiState: ActivityCheckinUiState,
    viewModel: ActivityCheckinViewModel,
    permissionManager: com.suseoaa.projectoaa.util.PlatformPermissionManager,
    onCheckinClick: (NearFieldCheckinTask) -> Unit,
    onRequestPermissions: () -> Unit,
    onTaskClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRestartClick: (NearFieldTask) -> Unit,
    selectedTaskForParticipants: String?,
    onDismissParticipants: () -> Unit,
    onManualAdd: () -> Unit,
    onSync: () -> Unit,
    onDeleteParticipant: (Long) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }) {
                    Text("发现任务", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }) {
                    Text("历史记录", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                DiscoveryView(
                    uiState = uiState,
                    viewModel = viewModel,
                    permissionManager = permissionManager,
                    onCheckinClick = onCheckinClick,
                    onRequestPermissions = onRequestPermissions
                )
            } else {
                HistoryView(
                    history = uiState.taskHistory,
                    onTaskClick = onTaskClick,
                    onDeleteClick = onDeleteClick,
                    onRestartClick = onRestartClick
                )
            }
        }

        // 签到成功弹窗
        if (uiState.checkinSuccess) {
            CheckinSuccessOverlay(
                taskName = uiState.lastCheckinTask?.activityName ?: "",
                onDismiss = { viewModel.resetCheckinStatus() }
            )
        }

        // 参与者详情底栏
        if (selectedTaskForParticipants != null) {
            val currentTask =
                uiState.taskHistory.find { it.taskIdentifier == selectedTaskForParticipants }
            val isMyHosted = currentTask?.isMyHosted == 1L

            ParticipantsBottomSheet(
                participants = uiState.currentParticipants,
                isMyHosted = isMyHosted,
                onDismiss = onDismissParticipants,
                onManualAdd = onManualAdd,
                onSync = onSync,
                onDeleteParticipant = onDeleteParticipant
            )
        }
    }
}
