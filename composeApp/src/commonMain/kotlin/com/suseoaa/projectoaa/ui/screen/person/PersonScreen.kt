package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
import com.suseoaa.projectoaa.presentation.person.PersonViewModel
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import com.suseoaa.projectoaa.presentation.update.UpdateEvent
import com.suseoaa.projectoaa.ui.component.UpdateDialog
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.pickImageForAvatar
import com.suseoaa.projectoaa.util.showToast
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

private val HeaderHeight = 320.dp

// 亮色渐变
private val LightGradientColors = listOf(
    Color(0xFF9BDCE5),
    Color(0xFF8EC5FC),
)

// 暗色渐变
private val DarkGradientColors = listOf(
    Color(0xFF1A3A4A),
    Color(0xFF1A2A4A),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToCheckin: () -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
    viewModel: PersonViewModel = koinViewModel(),
    updateViewModel: AppUpdateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 更新相关状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isManualUpdateCheck by remember { mutableStateOf(false) }
    val updateUiState by updateViewModel.uiState.collectAsState()

    // 头像选择对话框状态
    var showAvatarDialog by remember { mutableStateOf(false) }

    // 监听登出
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    // 启动时自动检查更新（使用自动检查方法，会检查是否已弹过窗）
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdateAuto()
    }

    // 监听更新事件
    LaunchedEffect(Unit) {
        updateViewModel.events.collectLatest { event ->
            when (event) {
                is UpdateEvent.DownloadComplete -> {
                    // 自动提示安装
                    updateViewModel.installDownloadedApk()
                }

                is UpdateEvent.NoUpdateAvailable -> {
                    // 无更新，可以显示 Snackbar
                }

                is UpdateEvent.ShowToast -> {
                    // 显示错误消息
                }
            }
        }
    }

    // 显示提示
    uiState.message?.let { message ->
        showToast(message)
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
    }

    // 自动弹出更新对话框（只在有更新且未弹过时弹出）
    LaunchedEffect(updateUiState.hasUpdate, updateUiState.hasShownAutoDialog) {
        if (updateUiState.hasUpdate && !updateUiState.hasShownAutoDialog) {
            showUpdateDialog = true
            isManualUpdateCheck = false
        }
    }

    // 更新对话框
    if (showUpdateDialog) {
        UpdateDialog(
            viewModel = updateViewModel,
            onDismiss = { showUpdateDialog = false },
            isManualCheck = isManualUpdateCheck
        )
    }

    // 头像选择
    if (showAvatarDialog) {
        pickImageForAvatar { imageData ->
            if (imageData != null) {
                viewModel.uploadAvatar(imageData)
            }
            showAvatarDialog = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        val isDarkTheme = isSystemInDarkTheme()
        val gradientColors = if (isDarkTheme) DarkGradientColors else LightGradientColors
        val headerTextColor = if (isDarkTheme) Color.White else Color.Black

        Box(modifier = Modifier.fillMaxSize()) {
            // 底层：动态背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = gradientColors + MaterialTheme.colorScheme.background
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "青蟹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "致力服务于四川轻化工大学开放原子开源协会",
                        style = MaterialTheme.typography.bodyMedium,
                        color = headerTextColor.copy(alpha = 0.5f)
                    )
                }
            }

            // 顶层：滚动内容
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AdaptiveLayout { config ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(config.getListColumns()),
                        contentPadding = PaddingValues(
                            top = 16.dp + statusBarHeight,
                            bottom = 16.dp + bottomBarHeight,
                            start = config.horizontalPadding,
                            end = config.horizontalPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(HeaderHeight - 80.dp))
                        }

                        // 用户信息卡片
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            UserInfoCard(
                                userInfo = uiState.userInfo,
                                onLogout = { viewModel.logout() },
                                onAvatarClick = { showAvatarDialog = true },
                                onEditInfo = { username, name, email ->
                                    viewModel.updateInfo(username, name, email)
                                }
                            )
                        }

                        // 修改密码卡片
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SettingCard(
                                icon = Icons.Default.Lock,
                                title = "修改密码",
                                subtitle = "更新您的账户密码",
                                onClick = onNavigateToChangePassword
                            )
                        }

                        // 检查更新卡片
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SettingCard(
                                icon = Icons.Default.Refresh,
                                title = "检查更新",
                                subtitle = if (updateUiState.isChecking) "正在检查..." else "点击检查是否有新版本",
                                onClick = {
                                    isManualUpdateCheck = true
                                    showUpdateDialog = true
                                    updateViewModel.checkForUpdate()
                                }
                            )
                        }

                        // 652签到入口（解锁后永久显示）
                        if (uiState.isCheckinUnlocked) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SettingCard(
                                    icon = Icons.Default.Edit,
                                    title = "652签到",
                                    subtitle = "快速签到打卡",
                                    onClick = onNavigateToCheckin
                                )
                            }
                        }

                        // 应用信息
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AppInfoCard(
                                isUnlocked = uiState.isCheckinUnlocked,
                                onSecretUnlocked = {
                                    viewModel.unlockCheckinFeature()
                                    onNavigateToCheckin()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserInfoCard(
    userInfo: com.suseoaa.projectoaa.data.model.PersonData?,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditInfo: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && userInfo != null) {
        EditInfoDialog(
            initialUsername = userInfo.username ?: "",
            initialName = userInfo.name ?: "",
            initialEmail = userInfo.email ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { username, name, email ->
                onEditInfo(username, name, email)
                showEditDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像区域
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    // 头像主体
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onAvatarClick() }
                    ) {
                        if (userInfo?.avatar.isNullOrBlank()) {
                            // 无头像时显示默认图标
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            // 有头像时加载图片
                            AsyncImage(
                                model = userInfo.avatar,
                                contentDescription = "用户头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // 编辑图标提示 - 放在头像外层
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 用户信息 (可点击编辑)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userInfo?.name ?: "请登录",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = userInfo?.department ?: "暂未加入任何部门",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userInfo?.role ?: "未加入协会",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }

                // 退出登录按钮
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "退出登录",
                        tint = AlertRed
                    )
                }
            }
        }
    }
}

/**
 * 编辑信息对话框
 */
@Composable
fun EditInfoDialog(
    initialUsername: String,
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("修改个人信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, name, email) },
                enabled = username.isNotBlank() && name.isNotBlank()
            ) {
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

@Composable
fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AppInfoCard(
    isUnlocked: Boolean = false,
    onSecretUnlocked: () -> Unit = {}
) {
    // 连续点击计数和时间追踪（仅在未解锁时使用）
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val resetTimeoutMs = 2000L // 2秒内需完成5次点击

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "青蟹",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 版本号 - 未解锁时可点击解锁隐藏功能
            Text(
                text = "版本 ${getAppVersionName()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (!isUnlocked) {
                    Modifier.clickable {
                        val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        // 如果距上次点击超过超时时间，重置计数
                        if (currentTime - lastClickTime > resetTimeoutMs) {
                            clickCount = 1
                        } else {
                            clickCount++
                        }
                        lastClickTime = currentTime

                        // 达到5次点击，触发隐藏功能
                        if (clickCount >= 5) {
                            clickCount = 0
                            onSecretUnlocked()
                        }
                    }
                } else {
                    Modifier
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "四川轻化工大学开放原子开源协会",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
