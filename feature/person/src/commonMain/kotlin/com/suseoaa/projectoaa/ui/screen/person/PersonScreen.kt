package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinViewModel
import com.suseoaa.projectoaa.presentation.person.PersonViewModel
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import com.suseoaa.projectoaa.presentation.update.UpdateEvent
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.component.UpdateDialog
import com.suseoaa.projectoaa.ui.screen.checkin.ScheduledCheckinDialog
import com.suseoaa.projectoaa.ui.theme.*
import com.suseoaa.projectoaa.util.pickImageForAvatar
import com.suseoaa.projectoaa.util.showToast
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import com.suseoaa.projectoaa.domain.checkin.SchedulerStatus
import com.suseoaa.projectoaa.ui.component.SettingRow
import com.suseoaa.projectoaa.ui.component.SettingGroupCard

// 「我的」页面：用户信息、各项设置入口与关于。

private val HeaderHeight = 320.dp

// 亮色渐变
private val LightGradientColors = listOf(
    Color(0xFF9BDCE5),
    Color(0xFF8EC5FC),
)

// 暗色渐变
private val DarkGradientColors = listOf(
    Color(0xFF15191D),
    Color(0xFF0D0F12),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
    viewModel: PersonViewModel = koinViewModel(),
    updateViewModel: AppUpdateViewModel = koinViewModel(),
    scheduledCheckinViewModel: ScheduledCheckinViewModel = koinViewModel()
) {
    val isMainTabVisible = LocalMainTabVisible.current
    val uiState by viewModel.uiState.collectAsState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scheduledCheckinUiState by scheduledCheckinViewModel.uiState.collectAsState()

    // 更新相关状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isManualUpdateCheck by remember { mutableStateOf(false) }
    val updateUiState by updateViewModel.uiState.collectAsState()

    // 头像选择对话框状态
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showStartTabDialog by remember { mutableStateOf(false) }
    var showScheduledCheckinDialog by remember { mutableStateOf(false) }

    // 监听登出
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    LaunchedEffect(uiState.isDynamicColorEnabled) {
        if (!uiState.isDynamicColorEnabled) {
            showPaletteDialog = false
        }
    }

    // 保活模式下，切回“个人”页时主动执行自动检查更新。
    LaunchedEffect(isMainTabVisible) {
        if (isMainTabVisible) {
            updateViewModel.checkForUpdateAuto()
        }
    }

    // 监听更新事件
    LaunchedEffect(Unit) {
        updateViewModel.events.collectLatest { event ->
            when (event) {
                is UpdateEvent.DownloadComplete -> {
                    // 下载完成，ViewModel 已自动拉起安装
                }

                is UpdateEvent.NoUpdateAvailable -> {
                    // 无更新
                }

                is UpdateEvent.ShowToast -> {
                    // 显示错误消息
                }
            }
        }
    }

    // 显示提示
    uiState.message?.let { message ->
        if (message.isNotBlank()) {
            showToast(message)
        }
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
    }

    // 自动弹出更新对话框（只在有更新且未弹过时弹出）
    LaunchedEffect(isMainTabVisible, updateUiState.hasUpdate, updateUiState.hasShownAutoDialog) {
        if (isMainTabVisible && updateUiState.hasUpdate && !updateUiState.hasShownAutoDialog && !showUpdateDialog) {
            showUpdateDialog = true
            isManualUpdateCheck = false
            // 标记该版本已弹过自动弹窗，下次不再自动弹出
            updateViewModel.markDialogShown()
        }
    }

    // 更新对话框
    if (showUpdateDialog && isMainTabVisible) {
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

    if (showPaletteDialog) {
        DynamicColorPaletteDialog(
            initialLightColorHex = uiState.dynamicPaletteLightColorHex,
            initialDarkColorHex = uiState.dynamicPaletteDarkColorHex,
            onDismiss = { showPaletteDialog = false },
            onApply = { lightHex, darkHex ->
                viewModel.setDynamicPaletteColors(lightHex, darkHex)
            },
            onConfirm = { lightHex, darkHex ->
                viewModel.setDynamicPaletteColors(lightHex, darkHex)
                showPaletteDialog = false
            }
        )
    }

    if (showStartTabDialog) {
        StartTabDialog(
            currentTab = uiState.defaultStartTab,
            onDismiss = { showStartTabDialog = false },
            onConfirm = { tabIndex ->
                viewModel.saveDefaultStartTab(tabIndex)
                showStartTabDialog = false
            }
        )
    }

    // 定时签到弹窗
    if (showScheduledCheckinDialog) {
        LaunchedEffect(Unit) {
            scheduledCheckinViewModel.show()
        }
        ScheduledCheckinDialog(
            uiState = scheduledCheckinUiState,
            onDismiss = {
                showScheduledCheckinDialog = false
                scheduledCheckinViewModel.dismiss()
            },
            onToggleEnabled = { scheduledCheckinViewModel.toggleEnabled() },
            onSetHour = { scheduledCheckinViewModel.setHour(it) },
            onSetMinute = { scheduledCheckinViewModel.setMinute(it) },
            onSetSecond = { scheduledCheckinViewModel.setSecond(it) },
            onSetRetryCount = { scheduledCheckinViewModel.setRetryCount(it) },
            onSetRetryInterval = { scheduledCheckinViewModel.setRetryInterval(it) },
            onToggleAccount = { scheduledCheckinViewModel.toggleAccount(it) },
            // saveConfig 会在 viewModelScope 里跑完保存和调度器的启停，不受弹窗关闭影响；
            // ViewModel 自己也会把 showDialog 置回 false，但这里的入口状态是独立的一份，
            // 不跟着收起来的话点完保存弹窗会一直留在屏幕上。
            onSave = {
                scheduledCheckinViewModel.saveConfig()
                showScheduledCheckinDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isDarkTheme = isSystemInDarkTheme()
        val gradientColors = if (isDarkTheme) DarkGradientColors else LightGradientColors
        val headerTextColor = if (isDarkTheme) Color.White else Color.Black
        val gridState = rememberLazyGridState()
        val density = LocalDensity.current
        val backgroundEffectRangePx = with(density) { (HeaderHeight - 80.dp).toPx() }

        val backgroundProgress by remember(gridState, backgroundEffectRangePx) {
            derivedStateOf {
                val scrolledPx = when {
                    gridState.firstVisibleItemIndex > 0 -> backgroundEffectRangePx
                    else -> gridState.firstVisibleItemScrollOffset.toFloat()
                }.coerceIn(0f, backgroundEffectRangePx)

                if (backgroundEffectRangePx <= 0f) 0f
                else (scrolledPx / backgroundEffectRangePx).coerceIn(0f, 1f)
            }
        }


        Box(modifier = Modifier.fillMaxSize()) {
            // 层1：全屏蔓延的渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = gradientColors
                        )
                    )
            )

            // 层2：固定的头部文字（随着滚动逐渐缩小、上移并淡出）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = 1f - (backgroundProgress * 0.14f)
                            scaleX = scale
                            scaleY = scale
                            // 加快淡出速度，确保被卡片完全覆盖前消失
                            alpha = (1f - backgroundProgress * 1.5f).coerceIn(0f, 1f)
                            translationY = with(density) { ((-18).dp).toPx() } * backgroundProgress
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "青蟹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "致力服务于四川轻化工大学开放原子开源协会",
                        style = MaterialTheme.typography.bodyMedium,
                        color = headerTextColor.copy(alpha = 0.5f)
                    )
                }
            }

            // 层3：纯色背景覆盖（根据滚动进度逐渐变为纯背景色）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundProgress))
            )

            // 顶层：滚动内容
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AdaptiveLayout { config ->
                    LazyVerticalGrid(
                        state = gridState,
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

                        // 功能入口组
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SettingGroupCard {
                                SettingRow(
                                    icon = Icons.Default.Lock,
                                    title = "修改密码",
                                    subtitle = "更新您的账户密码",
                                    modifier = Modifier.sharedBoundsTransition("change_password"),
                                    onClick = onNavigateToChangePassword
                                )
                                
                                if (uiState.isCheckinUnlocked) {
                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 80.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingRow(
                                        icon = Icons.Default.Edit,
                                        title = "652签到",
                                        subtitle = "快速签到打卡",
                                        modifier = Modifier.sharedBoundsTransition("checkin"),
                                        onClick = onNavigateToCheckin
                                    )
                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 80.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    val schedulerConfig = scheduledCheckinUiState.config
                                    SettingRow(
                                        icon = Icons.Default.Schedule,
                                        title = "定时签到",
                                        subtitle = if (schedulerConfig.enabled) {
                                            "每天 ${schedulerConfig.scheduledHour.toString().padStart(2, '0')}:${schedulerConfig.scheduledMinute.toString().padStart(2, '0')}:${schedulerConfig.scheduledSecond.toString().padStart(2, '0')} 自动签到 ${schedulerConfig.targetAccountIds.size} 个账号"
                                        } else {
                                            "未启用"
                                        },
                                        modifier = Modifier.sharedBoundsTransition("scheduled_checkin"),
                                        showBadge = scheduledCheckinUiState.schedulerStatus is SchedulerStatus.Running,
                                        onClick = { showScheduledCheckinDialog = true }
                                    )
                                }
                            }
                        }

                                // 系统设置组
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SettingGroupCard {
                                SettingRow(
                                    icon = Icons.Default.Refresh,
                                    title = "检查更新",
                                    subtitle = when {
                                        updateUiState.isChecking -> "正在检查..."
                                        updateUiState.hasUpdate && updateUiState.latestRelease != null ->
                                            "发现新版本 ${updateUiState.latestRelease!!.tagName}"

                                        else -> "当前已经是最新版本了"
                                    },
                                    modifier = Modifier.sharedBoundsTransition("update"),
                                    showBadge = updateUiState.hasUpdate && updateUiState.latestRelease != null,
                                    trailingText = if (updateUiState.hasUpdate && updateUiState.latestRelease != null)
                                        updateUiState.latestRelease!!.tagName else null,
                                    onClick = onNavigateToUpdate
                                )
                                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 80.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                SettingRow(
                                    icon = Icons.Default.Settings,
                                    title = "设置",
                                    subtitle = "界面、手势与个性化偏好",
                                    modifier = Modifier.sharedBoundsTransition("settings"),
                                    onClick = onNavigateToSettings
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

/**
 * 起始页选择对话框
 */
@Composable
private fun StartTabDialog(
    currentTab: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val tabOptions = listOf("首页", "课程", "教务信息", "个人")
    var selectedTab by remember { mutableIntStateOf(currentTab) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = {
            Text(
                "起始页设置",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    "选择打开应用时默认显示的页面",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                tabOptions.forEachIndexed { index, label ->
                    Surface(
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == index)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        tonalElevation = if (selectedTab == index) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedTab == index) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTab) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
