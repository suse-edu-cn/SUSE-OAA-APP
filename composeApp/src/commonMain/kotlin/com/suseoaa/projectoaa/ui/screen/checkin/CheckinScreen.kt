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
import com.suseoaa.projectoaa.presentation.checkin.CheckinViewModel
import org.koin.compose.viewmodel.koinViewModel

// 打卡状态颜色
private val SuccessGreen = Color(0xFF4CAF50)
private val InfoBlue = Color(0xFF2196F3)
private val ErrorRed = Color(0xFFF44336)

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
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示消息
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("652打卡") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 添加账号按钮
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "添加账号")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.accounts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.accounts.isEmpty()) {
                EmptyState(onAddClick = { viewModel.showAddDialog() })
            } else {
                // 账号列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            isChecking = uiState.currentCheckingAccount?.studentId == account.studentId,
                            onCheckin = { viewModel.startCheckin(account) },
                            onEdit = { viewModel.showEditDialog(account) },
                            onDelete = { viewModel.deleteAccount(account.id) }
                        )
                    }
                    
                    // 底部留白
                    item { Spacer(modifier = Modifier.height(80.dp)) }
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
    onDelete: () -> Unit
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
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
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
            SuccessGreen,
            Color.White,
            "已打卡"
        )
        status.startsWith("○") || status == "已签到" -> Triple(
            InfoBlue,
            Color.White,
            "已签到"
        )
        else -> Triple(
            ErrorRed,
            Color.White,
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
