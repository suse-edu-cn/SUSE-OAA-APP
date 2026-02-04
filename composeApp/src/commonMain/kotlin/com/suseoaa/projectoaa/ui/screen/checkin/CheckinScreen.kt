package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.data.model.CheckinAccountData
import com.suseoaa.projectoaa.data.model.CheckinLocations
import com.suseoaa.projectoaa.presentation.checkin.AccountFilterType
import com.suseoaa.projectoaa.presentation.checkin.CheckinViewModel
import com.suseoaa.projectoaa.presentation.checkin.QrCodeScanStatus
import com.suseoaa.projectoaa.util.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel

// 自定义颜色 - 适配暗色模式
@Composable
private fun getTaskPendingColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF90CAF9) // 浅蓝色 - 暗色模式
} else {
    Color(0xFF1976D2) // 深蓝色 - 亮色模式
}

@Composable
private fun getTaskCompletedColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF81C784) // 浅绿色 - 暗色模式
} else {
    Color(0xFF388E3C) // 深绿色 - 亮色模式
}

@Composable
private fun getTaskAbsentColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFFE57373) // 浅红色 - 暗色模式
} else {
    Color(0xFFD32F2F) // 深红色 - 亮色模式
}

@Composable
private fun getTaskPendingBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF1E3A5F) // 暗蓝色背景
} else {
    Color(0xFFE3F2FD) // 浅蓝色背景
}

@Composable
private fun getTaskCompletedBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF1B5E20) // 暗绿色背景
} else {
    Color(0xFFE8F5E9) // 浅绿色背景
}

@Composable
private fun getTaskAbsentBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF5F2120) // 暗红色背景
} else {
    Color(0xFFFFEBEE) // 浅红色背景
}

// 打卡状态颜色 - 已移除，改用MaterialTheme主题颜色适配暗色模式

/**
 * 652打卡页面（隐藏功能）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(
    onBack: () -> Unit,
    viewModel: CheckinViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 处理系统返回键：如果在任务列表页面，返回到账号列表；否则退出打卡页面
    PlatformBackHandler(enabled = uiState.selectedAccount != null) {
        viewModel.clearTasks()
    }

    // 显示消息 - 使用跨平台 Toast
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            com.suseoaa.projectoaa.util.ToastManager.showToast(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            com.suseoaa.projectoaa.util.ToastManager.showToast(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            // 只有在显示账号列表时才显示顶部栏，任务列表有自己的顶部栏
            if (uiState.selectedAccount == null) {
                TopAppBar(
                    title = { Text("652打卡") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        // 扫码添加按钮
                        IconButton(onClick = { viewModel.showQrCodeDialog() }) {
                            Icon(Icons.Default.QrCodeScanner, "扫码添加")
                        }
                        // 密码添加账号按钮
                        IconButton(onClick = { viewModel.showAddDialog() }) {
                            Icon(Icons.Default.Add, "添加账号")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 根据selectedAccount判断显示账号列表还是任务列表
            if (uiState.selectedAccount != null) {
                // 显示任务列表
                TaskListView(
                    viewModel = viewModel,
                    uiState = uiState,
                    onBack = { viewModel.clearTasks() }
                )
            } else {
                // 显示账号列表
                if (uiState.isLoading && uiState.accounts.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.accounts.isEmpty()) {
                    EmptyState(onAddClick = { viewModel.showAddDialog() })
                } else {
                    // 账号列表 - 支持平板适配和筛选
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 筛选栏
                        AccountFilterBar(
                            currentFilter = uiState.accountFilter,
                            onFilterChange = { viewModel.setAccountFilter(it) },
                            passwordCount = uiState.accounts.count { !it.isQrCodeLogin },
                            qrCodeCount = uiState.accounts.count { it.isQrCodeLogin }
                        )
                        
                        // 筛选后的账号列表
                        val filteredAccounts = viewModel.getFilteredAccounts()
                        
                        BoxWithConstraints(modifier = Modifier.weight(1f)) {
                            val isTablet = maxWidth > 600.dp
                            val columns = if (isTablet) 2 else 1

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    count = (filteredAccounts.size + columns - 1) / columns,
                                    key = { it }
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < filteredAccounts.size) {
                                                val account = filteredAccounts[index]
                                                Box(modifier = Modifier.weight(1f)) {
                                                    AccountCard(
                                                        account = account,
                                                        isChecking = uiState.currentCheckingAccount?.studentId == account.studentId,
                                                        onCheckin = { viewModel.startCheckin(account) },
                                                        onEdit = { viewModel.showEditDialog(account) },
                                                        onDelete = { viewModel.deleteAccount(account.id) },
                                                        onViewTasks = {
                                                            viewModel.loadTasksForAccount(
                                                                account
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // 底部留白
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                            
                            // 如果筛选后列表为空
                            if (filteredAccounts.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (uiState.accountFilter) {
                                            AccountFilterType.PASSWORD -> "暂无密码登录账号"
                                            AccountFilterType.QRCODE -> "暂无扫码登录账号"
                                            else -> "暂无账号"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // 批量打卡按钮 - 仅当有密码登录账号时显示
                        val passwordAccountCount = uiState.accounts.count { !it.isQrCodeLogin }
                        if (passwordAccountCount > 0 && uiState.accountFilter != AccountFilterType.QRCODE) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                tonalElevation = 3.dp
                            ) {
                                Button(
                                    onClick = { viewModel.batchCheckin() },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("正在批量打卡...")
                                    } else {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("批量打卡 ($passwordAccountCount 个密码账号)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加账号对话框
    if (uiState.showAddDialog) {
        AccountDialog(
            title = "添加账号",
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { studentId, password, name, remark ->
                viewModel.addAccount(studentId, password, name, remark)
            }
        )
    }

    // 编辑账号对话框
    if (uiState.showEditDialog && uiState.editingAccount != null) {
        AccountDialog(
            title = "编辑账号",
            initialAccount = uiState.editingAccount,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { studentId, password, name, remark ->
                viewModel.updateAccount(
                    uiState.editingAccount!!.id,
                    studentId, password, name, remark
                )
            }
        )
    }

    // 验证码对话框
    if (uiState.showCaptchaDialog) {
        CaptchaDialog(
            captchaImageBytes = uiState.captchaImageBytes,
            isLoading = uiState.isLoadingCaptcha,
            isLoggingIn = uiState.isLoggingIn,
            accountName = uiState.currentCheckingAccount?.name?.ifEmpty {
                uiState.currentCheckingAccount?.studentId
            } ?: "",
            onRefresh = { viewModel.refreshCaptcha() },
            onSubmit = { captchaCode -> viewModel.submitCaptchaAndCheckin(captchaCode) },
            onDismiss = { viewModel.cancelCheckin() }
        )
    }

    // WebView 扫码登录对话框
    if (uiState.showWebViewLoginDialog) {
        PlatformWebViewLoginDialog(
            onLoginSuccess = { cookies ->
                viewModel.onWebViewLoginSuccess(cookies)
            },
            onLoginError = { error ->
                viewModel.onWebViewLoginError(error)
            },
            onDismiss = { viewModel.hideWebViewLoginDialog() }
        )
    }

    // Session过期重新登录对话框
    if (uiState.showReloginDialog && uiState.accountNeedRelogin != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideReloginDialog() },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("登录已过期") },
            text = {
                Text("账号 ${uiState.accountNeedRelogin?.name?.ifEmpty { uiState.accountNeedRelogin?.studentId }} 的登录已过期，需要重新扫码登录。")
            },
            confirmButton = {
                Button(onClick = { viewModel.startRelogin() }) {
                    Text("重新扫码")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideReloginDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无打卡账号",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮添加账号",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加账号")
        }
    }
}

/**
 * 账号卡片
 */
@Composable
private fun AccountCard(
    account: CheckinAccountData,
    isChecking: Boolean,
    onCheckin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewTasks: () -> Unit = {}  // 新增：查看任务回调
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 头部：学号和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (account.name.isNotEmpty())
                                account.name.take(1)
                            else
                                account.studentId.takeLast(2),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (account.name.isNotEmpty()) account.name else account.studentId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (account.name.isNotEmpty()) {
                            Text(
                                text = account.studentId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 状态指示器
                StatusChip(
                    status = account.lastCheckinStatus,
                    isChecking = isChecking
                )
            }

            // 备注
            if (account.remark.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.remark,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 登录类型和签到地点
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 登录类型标签
                Surface(
                    color = if (account.isQrCodeLogin)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (account.isQrCodeLogin) "扫码登录" else "密码登录",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (account.isQrCodeLogin)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // 签到地点标签（仅扫码登录显示）
                if (account.isQrCodeLogin) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = account.selectedLocation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 上次打卡时间
            if (account.lastCheckinTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "上次打卡: ${account.lastCheckinTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // 操作按钮
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 单独打卡
                FilledTonalButton(
                    onClick = onCheckin,
                    enabled = !isChecking,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打卡")
                }

                // 查看任务（所有账号都可以查看）
                OutlinedButton(
                    onClick = onViewTasks,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("任务")
                }

                // 编辑
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 删除
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除账号 ${account.name.ifEmpty { account.studentId }} 吗？此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 状态标签
 */
@Composable
private fun StatusChip(
    status: String?,
    isChecking: Boolean
) {
    val (backgroundColor, contentColor, text) = when {
        isChecking -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "打卡中..."
        )

        status == null -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "未打卡"
        )

        status.startsWith("✓") || status == "成功" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "已打卡"
        )

        status.startsWith("○") || status == "已签到" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "已签到"
        )

        else -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            status.removePrefix("✗ ").take(6)
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 账号编辑对话框
 */
@Composable
private fun AccountDialog(
    title: String,
    initialAccount: CheckinAccountData? = null,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, password: String, name: String, remark: String) -> Unit
) {
    var studentId by remember { mutableStateOf(initialAccount?.studentId ?: "") }
    var password by remember { mutableStateOf(initialAccount?.password ?: "") }
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var remark by remember { mutableStateOf(initialAccount?.remark ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("学号 *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = initialAccount == null, // 编辑时不允许修改学号
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 *") },
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                tint = if (showPassword)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(studentId, password, name, remark) },
                enabled = studentId.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 验证码输入对话框
 */
@Composable
private fun CaptchaDialog(
    captchaImageBytes: ByteArray?,
    isLoading: Boolean,
    isLoggingIn: Boolean,
    accountName: String,
    onRefresh: () -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var captchaCode by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 当验证码图片加载后自动进行OCR识别
    LaunchedEffect(captchaImageBytes) {
        if (captchaImageBytes != null && captchaImageBytes.isNotEmpty()) {
            isRecognizing = true
            ocrError = null
            try {
                val result =
                    com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaImageBytes)
                result.onSuccess { recognizedCode ->
                    if (recognizedCode.length == 4) {
                        captchaCode = recognizedCode
                        println("[OCR] 自动识别成功: $recognizedCode")
                    } else {
                        println("[OCR] 识别结果长度不正确: $recognizedCode (长度: ${recognizedCode.length})")
                        ocrError = "识别结果异常，请手动输入"
                    }
                }.onFailure { e ->
                    println("[OCR] 识别失败: ${e.message}")
                    ocrError = "识别失败，请手动输入"
                }
            } catch (e: Exception) {
                println("[OCR] 识别异常: ${e.message}")
                ocrError = "识别异常，请手动输入"
            }
            isRecognizing = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        title = {
            Text("输入验证码")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 账号信息
                Text(
                    text = "正在为 $accountName 打卡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 验证码图片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isLoading && !isLoggingIn) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        captchaImageBytes != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(captchaImageBytes),
                                contentDescription = "验证码",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        else -> {
                            Text(
                                text = "点击加载验证码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 刷新提示
                Text(
                    text = "点击图片刷新验证码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // OCR识别状态
                if (isRecognizing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在自动识别验证码...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (captchaCode.length == 4 && ocrError == null) {
                    Text(
                        text = "已自动识别验证码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (ocrError != null) {
                    Text(
                        text = ocrError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 验证码输入框
                OutlinedTextField(
                    value = captchaCode,
                    onValueChange = {
                        // 只允许输入数字和字母，最多4位
                        if (it.length <= 4 && it.all { c -> c.isLetterOrDigit() }) {
                            captchaCode = it
                        }
                    },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入4位验证码") },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (captchaCode.length == 4 && !isLoggingIn) {
                                onSubmit(captchaCode)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 登录中指示
                if (isLoggingIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在登录并打卡...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(captchaCode) },
                enabled = captchaCode.length == 4 && !isLoggingIn
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoggingIn
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 扫码登录对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrCodeLoginDialog(
    qrCodeUrl: String?,
    isLoading: Boolean,
    scanStatus: QrCodeScanStatus,
    scannedStudentId: String?,
    scannedName: String?,
    onRefresh: () -> Unit,
    onConfirm: (studentId: String, password: String, name: String, location: String) -> Unit,
    onDismiss: () -> Unit
) {
    // 当扫码成功后自动填充用户信息
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf(CheckinLocations.DEFAULT.name) }
    var showLocationDropdown by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // 当扫码成功时自动填充
    LaunchedEffect(scannedStudentId, scannedName) {
        if (!scannedStudentId.isNullOrBlank()) {
            studentId = scannedStudentId
        }
        if (!scannedName.isNullOrBlank()) {
            name = scannedName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加签到账号") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 二维码图片区域（辅助查看学号）
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isLoading) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "正在加载二维码...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        qrCodeUrl != null -> {
                            // 解码 base64 图片 - 使用跨平台工具
                            val imageBitmap = remember(qrCodeUrl) {
                                com.suseoaa.projectoaa.util.PlatformImageUtils.decodeBase64ToImageBitmap(
                                    qrCodeUrl
                                )
                            }

                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "微信扫码登录",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = "二维码加载失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "点击加载二维码",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 扫码提示
                Text(
                    text = when (scanStatus) {
                        QrCodeScanStatus.WAITING -> "请使用微信扫描上方二维码登录后查看学号"
                        QrCodeScanStatus.SCANNED -> "已扫描，请在微信中确认"
                        QrCodeScanStatus.CONFIRMED -> if (scannedStudentId != null) "扫码成功！已获取用户信息" else "确认成功！请填写信息"
                        QrCodeScanStatus.EXPIRED -> "二维码已过期，请点击刷新"
                        QrCodeScanStatus.ERROR -> "加载失败，请点击刷新"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (scanStatus) {
                        QrCodeScanStatus.EXPIRED, QrCodeScanStatus.ERROR -> MaterialTheme.colorScheme.error
                        QrCodeScanStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                HorizontalDivider()

                // 说明文字
                Text(
                    text = "扫码登录后，在微信页面中查看您的学号并填写下方",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("学号 *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 签到地点选择
                ExposedDropdownMenuBox(
                    expanded = showLocationDropdown,
                    onExpandedChange = { showLocationDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedLocation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("签到地点") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLocationDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showLocationDropdown,
                        onDismissRequest = { showLocationDropdown = false }
                    ) {
                        CheckinLocations.ALL.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    selectedLocation = location.name
                                    showLocationDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(studentId, password, name, selectedLocation) },
                enabled = studentId.isNotBlank() && password.isNotBlank()
            ) {
                Text("添加账号")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 任务列表视图
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListView(
    viewModel: CheckinViewModel,
    uiState: com.suseoaa.projectoaa.presentation.checkin.CheckinUiState,
    onBack: () -> Unit
) {
    val account = uiState.selectedAccount ?: return
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name.ifEmpty { account.studentId },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "待打卡: ${uiState.pendingTasks.size} | 已打卡: ${uiState.completedTasks.size} | 缺勤: ${uiState.absentTasks.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.loadTasksForAccount(account) }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        }

        // 筛选Tab
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("全部\n(${uiState.pendingTasks.size + uiState.completedTasks.size + uiState.absentTasks.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("待打卡\n(${uiState.pendingTasks.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("已打卡\n(${uiState.completedTasks.size})") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("缺勤\n(${uiState.absentTasks.size})") }
            )
        }

        if (uiState.isLoadingTasks) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth > 600.dp
                val columns = if (isTablet) 2 else 1

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 根据选中Tab显示不同的任务
                    when (selectedTab) {
                        0 -> {
                            // 全部任务
                            if (uiState.pendingTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "待打卡任务 (${uiState.pendingTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskPendingColor()
                                    )
                                }
                                items(
                                    count = (uiState.pendingTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.pendingTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.pendingTasks[index],
                                                        status = 1,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.pendingTasks[index],
                                                                allowRepeat = false
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.completedTasks.isNotEmpty()) {
                                // 只显示 displayedCompletedCount 个任务
                                val displayCount = minOf(uiState.displayedCompletedCount, uiState.completedTasks.size)
                                val hasMore = uiState.completedTasks.size > uiState.displayedCompletedCount
                                
                                item {
                                    Text(
                                        text = "已打卡任务 (${uiState.completedTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskCompletedColor()
                                    )
                                }
                                items(
                                    count = (displayCount + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < displayCount) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.completedTasks[index],
                                                        status = 2,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.completedTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                                
                                // 加载更多按钮
                                if (hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.loadMoreCompletedTasks() },
                                                enabled = !uiState.isLoadingMoreCompleted,
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                if (uiState.isLoadingMoreCompleted) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("加载中...")
                                                } else {
                                                    Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.absentTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "缺勤任务 (${uiState.absentTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskAbsentColor()
                                    )
                                }
                                items(
                                    count = (uiState.absentTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.absentTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.absentTasks[index],
                                                        status = 3,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.absentTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.pendingTasks.isEmpty() && uiState.completedTasks.isEmpty() && uiState.absentTasks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // 仅待打卡任务
                            if (uiState.pendingTasks.isNotEmpty()) {
                                items(
                                    count = (uiState.pendingTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.pendingTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.pendingTasks[index],
                                                        status = 1,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.pendingTasks[index],
                                                                allowRepeat = false
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无待打卡任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // 仅已打卡任务
                            if (uiState.completedTasks.isNotEmpty()) {
                                // 只显示 displayedCompletedCount 个任务
                                val displayCount = minOf(uiState.displayedCompletedCount, uiState.completedTasks.size)
                                val hasMore = uiState.completedTasks.size > uiState.displayedCompletedCount
                                
                                items(
                                    count = (displayCount + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < displayCount) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.completedTasks[index],
                                                        status = 2,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.completedTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                                
                                // 加载更多按钮
                                if (hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.loadMoreCompletedTasks() },
                                                enabled = !uiState.isLoadingMoreCompleted,
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                if (uiState.isLoadingMoreCompleted) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("加载中...")
                                                } else {
                                                    Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无已打卡任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // 仅缺勤任务
                            if (uiState.absentTasks.isNotEmpty()) {
                                items(
                                    count = (uiState.absentTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.absentTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.absentTasks[index],
                                                        status = 3,
                                                        isChecking = uiState.isLoading,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.absentTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无缺勤任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    if (uiState.pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "待打卡任务 (${uiState.pendingTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskPendingColor()
                            )
                        }
                        items(
                            count = (uiState.pendingTasks.size + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < uiState.pendingTasks.size) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.pendingTasks[index],
                                                status = 1,
                                                isChecking = uiState.isLoading,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.pendingTasks[index],
                                                        allowRepeat = false
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 缺勤任务（未打卡）
                    if (uiState.absentTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "缺勤任务 (${uiState.absentTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskAbsentColor()
                            )
                        }
                        items(
                            count = (uiState.absentTasks.size + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < uiState.absentTasks.size) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.absentTasks[index],
                                                status = 3,
                                                isChecking = uiState.isLoading,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.absentTasks[index],
                                                        allowRepeat = true
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 已打卡任务
                    if (uiState.completedTasks.isNotEmpty()) {
                        // 只显示 displayedCompletedCount 个任务
                        val displayCount = minOf(uiState.displayedCompletedCount, uiState.completedTasks.size)
                        val hasMore = uiState.completedTasks.size > uiState.displayedCompletedCount
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已打卡任务 (${uiState.completedTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskCompletedColor()
                            )
                        }
                        items(
                            count = (displayCount + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < displayCount) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.completedTasks[index],
                                                status = 2,
                                                isChecking = uiState.isLoading,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.completedTasks[index],
                                                        allowRepeat = true
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        
                        // 加载更多按钮
                        if (hasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.loadMoreCompletedTasks() },
                                        enabled = !uiState.isLoadingMoreCompleted,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        if (uiState.isLoadingMoreCompleted) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("加载中...")
                                        } else {
                                            Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 空状态
                    if (uiState.pendingTasks.isEmpty() && uiState.completedTasks.isEmpty() && uiState.absentTasks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无任务",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/**
 * 任务卡片
 */
@Composable
private fun TaskCard(
    task: com.suseoaa.projectoaa.data.model.CheckinTask,
    status: Int,  // 1=待打卡, 2=已打卡, 3=缺勤
    isChecking: Boolean,
    onCheckin: () -> Unit
) {
    val statusText = when (status) {
        1 -> "待打卡"
        2 -> "已打卡"
        3 -> "缺勤"
        else -> "未知"
    }

    val statusColor = when (status) {
        1 -> getTaskPendingColor()
        2 -> getTaskCompletedColor()
        3 -> getTaskAbsentColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardColor = when (status) {
        1 -> getTaskPendingBgColor()
        2 -> getTaskCompletedBgColor()
        3 -> getTaskAbsentBgColor()
        else -> MaterialTheme.colorScheme.surface
    }

    val statusTextColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.White
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == 2) 0.dp else 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 任务名称和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.rwmc,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 显示完整日期和时间
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = task.needTime.ifEmpty { task.qdksrq },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${task.qdkssj} - ${task.qdjssj}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 显示已打卡时间（如果有）
                    if (status == 2 && !task.qdsj.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = getTaskCompletedColor()
                            )
                            Text(
                                text = "打卡于 ${task.qdsj.substringAfter(" ").take(5)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = getTaskCompletedColor()
                            )
                        }
                    }
                }

                // 状态标签
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (status == 2 && !task.qdsj.isNullOrBlank()) {
                            "已打卡"
                        } else {
                            statusText
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // 打卡按钮
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCheckin,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (status == 2 || status == 3) "再次打卡中..." else "打卡中...")
                } else {
                    Icon(
                        if (status == 2 || status == 3) Icons.Default.Refresh else Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (status == 2 || status == 3) "再次打卡" else "立即打卡")
                }
            }
        }
    }
}

/**
 * 账号筛选栏
 */
@Composable
private fun AccountFilterBar(
    currentFilter: AccountFilterType,
    onFilterChange: (AccountFilterType) -> Unit,
    passwordCount: Int,
    qrCodeCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 全部
            FilterChip(
                selected = currentFilter == AccountFilterType.ALL,
                onClick = { onFilterChange(AccountFilterType.ALL) },
                label = { Text("全部 (${passwordCount + qrCodeCount})") },
                leadingIcon = if (currentFilter == AccountFilterType.ALL) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                } else null
            )
            
            // 密码登录
            FilterChip(
                selected = currentFilter == AccountFilterType.PASSWORD,
                onClick = { onFilterChange(AccountFilterType.PASSWORD) },
                label = { Text("密码 ($passwordCount)") },
                leadingIcon = if (currentFilter == AccountFilterType.PASSWORD) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                } else {
                    { Icon(Icons.Default.Password, null, modifier = Modifier.size(18.dp)) }
                }
            )
            
            // 扫码登录
            FilterChip(
                selected = currentFilter == AccountFilterType.QRCODE,
                onClick = { onFilterChange(AccountFilterType.QRCODE) },
                label = { Text("扫码 ($qrCodeCount)") },
                leadingIcon = if (currentFilter == AccountFilterType.QRCODE) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                } else {
                    { Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp)) }
                }
            )
        }
    }
}
