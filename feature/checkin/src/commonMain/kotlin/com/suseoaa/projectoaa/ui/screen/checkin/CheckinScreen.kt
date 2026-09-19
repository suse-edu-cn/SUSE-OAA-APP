package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.checkin.AccountFilterType
import com.suseoaa.projectoaa.presentation.checkin.CheckinViewModel
import com.suseoaa.projectoaa.presentation.checkin.filteredAccounts
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.ui.animation.pageShellBounds
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getDetailColumns
import com.suseoaa.projectoaa.util.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.ui.screen.checkin.dialogs.AccountDialog
import com.suseoaa.projectoaa.ui.screen.checkin.dialogs.CaptchaDialog
import com.suseoaa.projectoaa.ui.screen.checkin.dialogs.SmsVerificationDialog

// 签到主页面：账号列表、筛选栏与各对话框的编排入口。

/**
 * 652打卡页面（隐藏功能）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(
    onBack: () -> Unit,
    onNavigateToTasks: (CheckinAccountData) -> Unit = {},
    showInlineTasks: Boolean = true,
    viewModel: CheckinViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 处理系统返回键：如果在任务列表页面，返回到账号列表；否则退出打卡页面
    PlatformBackHandler(enabled = showInlineTasks && uiState.selectedAccount != null) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageShellBounds("checkin")
    ) {
        // 只有在显示账号列表时才显示顶部栏，任务列表有自己的顶部栏
        if (!showInlineTasks || uiState.selectedAccount == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Text(
                    "652打卡",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                // 扫码添加按钮
                IconButton(onClick = { viewModel.showQrCodeDialog() }) {
                    Icon(Icons.Default.QrCodeScanner, "扫码添加")
                }
                // 密码添加账号按钮
                IconButton(onClick = { viewModel.showAddDialog() }) {
                    Icon(Icons.Default.Add, "添加账号")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = if (showInlineTasks) uiState.selectedAccount else null,
                label = "checkin_account_transition"
            ) { targetAccount ->
                val content: @Composable () -> Unit = {
                    // 根据 targetAccount 判断显示账号列表还是任务列表
                    if (targetAccount != null) {
                        // 显示任务列表
                        TaskListView(
                            viewModel = viewModel,
                            uiState = uiState,
                            account = targetAccount,
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
                                    qrCodeCount = uiState.accounts.count { it.isQrCodeLogin },
                                    yibinCount = uiState.accounts.count { it.selectedLocation == "宜宾" },
                                    libaiheCount = uiState.accounts.count { it.selectedLocation == "李白河" },
                                    huidongCount = uiState.accounts.count { it.selectedLocation == "汇东" }
                                )

                                // 筛选后的账号列表
                                val filteredAccounts = viewModel.getFilteredAccounts()

                                AdaptiveLayout(modifier = Modifier.weight(1f)) { adaptiveLayoutConfig ->
                                    val columns = adaptiveLayoutConfig.getDetailColumns()

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
                                                                onCheckin = {
                                                                    viewModel.startCheckin(
                                                                        account
                                                                    )
                                                                },
                                                                onEdit = {
                                                                    viewModel.showEditDialog(
                                                                        account
                                                                    )
                                                                },
                                                                onDelete = {
                                                                    viewModel.deleteAccount(
                                                                        account.id
                                                                    )
                                                                },
                                                                onViewTasks = {
                                                                    if (showInlineTasks) {
                                                                        viewModel.loadTasksForAccount(account)
                                                                    } else {
                                                                        onNavigateToTasks(account)
                                                                    }
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
                                                    AccountFilterType.CAMPUS_YIBIN -> "暂无宜宾校区账号"
                                                    AccountFilterType.CAMPUS_LIBAIHE -> "暂无李白河校区账号"
                                                    AccountFilterType.CAMPUS_HUIDONG -> "暂无汇东校区账号"
                                                    else -> "暂无账号"
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 批量打卡按钮 - 仅当有密码登录账号时显示
                                val passwordAccountCount =
                                    uiState.accounts.count { !it.isQrCodeLogin }
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
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("批量打卡 ($passwordAccountCount 个密码账号)")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // End of else
                }

                if (showInlineTasks) {
                    CompositionLocalProvider(
                        com.suseoaa.projectoaa.ui.animation.LocalAnimatedVisibilityScope provides this@AnimatedContent
                    ) {
                        content()
                    }
                } else {
                    content()
                }
            } // End of AnimatedContent lambda
        }
    }

    // 添加账号对话框
    if (uiState.showAddDialog) {
        AccountDialog(
            title = "添加账号",
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { studentId, password, name, remark, campus ->
                viewModel.addAccount(studentId, password, name, remark, campus)
            }
        )
    }

    // 编辑账号对话框
    if (uiState.showEditDialog && uiState.editingAccount != null) {
        AccountDialog(
            title = "编辑账号",
            initialAccount = uiState.editingAccount,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { studentId, password, name, remark, campus ->
                viewModel.updateAccount(
                    uiState.editingAccount!!.id,
                    studentId, password, name, remark, campus
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

    // 短信二次验证对话框
    if (uiState.showSmsDialog) {
        SmsVerificationDialog(
            accountName = uiState.currentCheckingAccount?.name?.ifEmpty {
                uiState.currentCheckingAccount?.studentId
            } ?: "",
            maskedPhone = uiState.smsMaskedPhone,
            isSendingSms = uiState.isSendingSmsCode,
            isVerifying = uiState.isVerifyingSmsCode,
            smsResendCountdownSeconds = uiState.smsResendCountdownSeconds,
            onSendSms = { viewModel.sendSmsCode() },
            onSubmit = { smsCode -> viewModel.submitSmsCodeAndCheckin(smsCode) },
            onDismiss = { viewModel.cancelSmsVerification() }
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
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { viewModel.hideReloginDialog() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("登录已过期") },
            text = {
                Text(
                    "账号 " +
                            "${
                                uiState.accountNeedRelogin?.name?.ifEmpty
                                { uiState.accountNeedRelogin?.studentId }
                            } 的登录已过期，需要重新扫码登录。"
                )
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
            Icon(
                Icons.Default.Add,
                null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加账号")
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
    qrCodeCount: Int,
    yibinCount: Int,
    libaiheCount: Int,
    huidongCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 第一行：按登录类型筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

            // 第二行：按校区筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 宜宾
                FilterChip(
                    selected = currentFilter == AccountFilterType.CAMPUS_YIBIN,
                    onClick = { onFilterChange(AccountFilterType.CAMPUS_YIBIN) },
                    label = { Text("宜宾 ($yibinCount)") },
                    leadingIcon = if (currentFilter == AccountFilterType.CAMPUS_YIBIN) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else {
                        { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
                    }
                )

                // 李白河
                FilterChip(
                    selected = currentFilter == AccountFilterType.CAMPUS_LIBAIHE,
                    onClick = { onFilterChange(AccountFilterType.CAMPUS_LIBAIHE) },
                    label = { Text("李白河 ($libaiheCount)") },
                    leadingIcon = if (currentFilter == AccountFilterType.CAMPUS_LIBAIHE) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else {
                        { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
                    }
                )

                // 汇东
                FilterChip(
                    selected = currentFilter == AccountFilterType.CAMPUS_HUIDONG,
                    onClick = { onFilterChange(AccountFilterType.CAMPUS_HUIDONG) },
                    label = { Text("汇东 ($huidongCount)") },
                    leadingIcon = if (currentFilter == AccountFilterType.CAMPUS_HUIDONG) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else {
                        { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
                    }
                )
            }
        }
    }
}
