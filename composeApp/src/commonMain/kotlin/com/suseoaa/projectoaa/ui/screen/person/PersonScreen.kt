@file:Suppress("UnusedAssignment", "SpellCheckingInspection")

package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getListColumns
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
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
import kotlin.math.abs
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

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

private data class DynamicColorPaletteOption(
    val label: String,
    val color: Color
)

private val DynamicColorPaletteOptions = listOf(
    DynamicColorPaletteOption("电光蓝", ElectricBlue),
    DynamicColorPaletteOption("薄荷青", Color(0xFF00BFA5)),
    DynamicColorPaletteOption("天青", Color(0xFF00BCD4)),
    DynamicColorPaletteOption("湖蓝", Color(0xFF42A5F5)),
    DynamicColorPaletteOption("深海", Color(0xFF1565C0)),
    DynamicColorPaletteOption("夜蓝", NightBlue),
    DynamicColorPaletteOption("珊瑚", Color(0xFFFF8A65)),
    DynamicColorPaletteOption("胭脂", AlertRed),
    DynamicColorPaletteOption("玫瑰", Color(0xFFE9698B)),
    DynamicColorPaletteOption("紫藤", Color(0xFF7C6EF5)),
    DynamicColorPaletteOption("暖橙", Color(0xFFFFA447)),
    DynamicColorPaletteOption("柠黄", Color(0xFFFFD84D)),
    DynamicColorPaletteOption("青柠", Color(0xFF8BC34A)),
    DynamicColorPaletteOption("石墨", Color(0xFF546E7A)),
    DynamicColorPaletteOption("银灰", Color(0xFF90A4AE))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAiLab: () -> Unit = {},
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
            onSave = { scheduledCheckinViewModel.saveConfig() }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
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
                                        showBadge = scheduledCheckinUiState.schedulerStatus is com.suseoaa.projectoaa.presentation.checkin.SchedulerStatus.Running,
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
                                    icon = Icons.Default.AutoAwesome,
                                    title = "AI 实验室",
                                    subtitle = "本地 AI 功能 · 调课摘要、学业分析、智能查询",
                                    modifier = Modifier.sharedBoundsTransition("ai_lab"),
                                    onClick = onNavigateToAiLab
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

@Composable
private fun DynamicColorPaletteEntryCard(
    lightColorHex: String?,
    darkColorHex: String?,
    dynamicColorEnabled: Boolean,
    onClick: () -> Unit
) {
    val lightColor = lightColorHex.toColorOrNull() ?: defaultPaletteColor(isDarkMode = false)
    val darkColor = darkColorHex.toColorOrNull() ?: defaultPaletteColor(isDarkMode = true)
    val hasCustomPalette = !lightColorHex.isNullOrBlank() || !darkColorHex.isNullOrBlank()
    val modeDescription = when {
        hasCustomPalette -> "\n当前：自定义色优先"
        dynamicColorEnabled -> "\n当前：跟随系统莫奈"
        else -> "\n当前：跟随默认主题"
    }

    SettingCard(
        icon = Icons.Default.Palette,
        title = "莫奈调色盘",
        subtitle = "亮色 ${if (lightColorHex == null) "自动" else lightColor.toHexString()} / 暗色 ${if (darkColorHex == null) "自动" else darkColor.toHexString()} $modeDescription",
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(lightColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(darkColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicColorPaletteDialog(
    initialLightColorHex: String?,
    initialDarkColorHex: String?,
    onDismiss: () -> Unit,
    onApply: (String?, String?) -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) }
    var lightUseDefault by remember(initialLightColorHex) { mutableStateOf(initialLightColorHex.isNullOrBlank()) }
    var darkUseDefault by remember(initialDarkColorHex) { mutableStateOf(initialDarkColorHex.isNullOrBlank()) }
    var lightColor by remember(initialLightColorHex) {
        mutableStateOf(
            initialLightColorHex.toColorOrNull() ?: defaultPaletteColor(isDarkMode = false)
        )
    }
    var darkColor by remember(initialDarkColorHex) {
        mutableStateOf(
            initialDarkColorHex.toColorOrNull() ?: defaultPaletteColor(isDarkMode = true)
        )
    }

    val currentColor = if (selectedMode == 0) lightColor else darkColor
    val currentUseDefault = if (selectedMode == 0) lightUseDefault else darkUseDefault

    fun updateCurrentColor(newColor: Color) {
        if (selectedMode == 0) {
            lightUseDefault = false
            lightColor = newColor
        } else {
            darkUseDefault = false
            darkColor = newColor
        }
    }

    fun resetCurrentToDefault() {
        if (selectedMode == 0) {
            lightUseDefault = true
            lightColor = defaultPaletteColor(isDarkMode = false)
        } else {
            darkUseDefault = true
            darkColor = defaultPaletteColor(isDarkMode = true)
        }
    }

    fun buildCurrentPalette(): Pair<String?, String?> {
        return Pair(
            first = if (lightUseDefault) null else lightColor.toHexString(),
            second = if (darkUseDefault) null else darkColor.toHexString()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .fillMaxHeight(0.92f)
                .heightIn(max = 760.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "莫奈调色盘",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "关闭"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PrimaryTabRow(selectedTabIndex = selectedMode) {
                    Tab(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        text = { Text("亮色模式") }
                    )
                    Tab(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        text = { Text("暗色模式") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    DynamicColorPaletteEditor(
                        color = currentColor,
                        useDefault = currentUseDefault,
                        onColorChange = ::updateCurrentColor,
                        onResetDefault = ::resetCurrentToDefault
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    OutlinedButton(
                        onClick = {
                            val (lightHex, darkHex) = buildCurrentPalette()
                            onApply(lightHex, darkHex)
                        }
                    ) {
                        Text("立即应用")
                    }
                    Button(
                        onClick = {
                            val (lightHex, darkHex) = buildCurrentPalette()
                            onConfirm(lightHex, darkHex)
                        }
                    ) {
                        Text("保存并关闭")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DynamicColorPaletteEditor(
    color: Color,
    useDefault: Boolean,
    onColorChange: (Color) -> Unit,
    onResetDefault: () -> Unit
) {
    var parseInput by remember(color, useDefault) {
        mutableStateOf(if (useDefault) "" else color.toHexString())
    }
    var parseError by remember { mutableStateOf<String?>(null) }

    val hsv = color.toHsvColor()
    var hue by remember(color) { mutableFloatStateOf(hsv.h) }
    var saturation by remember(color) { mutableFloatStateOf(hsv.s) }
    var value by remember(color) { mutableFloatStateOf(hsv.v) }
    var alpha by remember(color) { mutableFloatStateOf(hsv.a) }

    fun updateByHsv(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newValue: Float = value,
        newAlpha: Float = alpha
    ) {
        hue = newHue.coerceIn(0f, 360f)
        saturation = newSaturation.coerceIn(0f, 1f)
        value = newValue.coerceIn(0f, 1f)
        alpha = newAlpha.coerceIn(0f, 1f)

        val updatedColor = Color.hsv(hue, saturation, value, alpha)
        onColorChange(updatedColor)
        parseInput = updatedColor.toHexString()
        parseError = null
    }

    val argb = color.toArgb()
    var redText by remember(color) { mutableStateOf(((argb ushr 16) and 0xFF).toString()) }
    var greenText by remember(color) { mutableStateOf(((argb ushr 8) and 0xFF).toString()) }
    var blueText by remember(color) { mutableStateOf((argb and 0xFF).toString()) }
    var alphaText by remember(color) { mutableStateOf(((argb ushr 24) and 0xFF).toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (useDefault) "当前使用默认色" else "当前颜色：${color.toHexString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "支持 HEX、RGB/RGBA、HSL/HSLA 输入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onResetDefault) {
                Text("恢复默认")
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DynamicColorPaletteOptions.forEach { option ->
                val isSelected = abs(option.color.toArgb() - color.toArgb()) <= 2
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable {
                            val optionHsv = option.color.toHsvColor()
                            updateByHsv(optionHsv.h, optionHsv.s, optionHsv.v, optionHsv.a)
                        }
                )
            }
        }

        SaturationValuePicker(
            hue = hue,
            saturation = saturation,
            value = value,
            onSaturationValueChange = { s, v -> updateByHsv(newSaturation = s, newValue = v) }
        )

        Text(
            text = "色相 ${hue.roundToInt()}°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = hue,
            onValueChange = { updateByHsv(newHue = it) },
            valueRange = 0f..360f
        )

        Text(
            text = "透明度 ${(alpha * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = alpha,
            onValueChange = { updateByHsv(newAlpha = it) },
            valueRange = 0f..1f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorChannelField(
                value = redText,
                label = "R",
                modifier = Modifier.weight(1f)
            ) { redText = it }
            ColorChannelField(
                value = greenText,
                label = "G",
                modifier = Modifier.weight(1f)
            ) { greenText = it }
            ColorChannelField(
                value = blueText,
                label = "B",
                modifier = Modifier.weight(1f)
            ) { blueText = it }
            ColorChannelField(
                value = alphaText,
                label = "A",
                modifier = Modifier.weight(1f)
            ) { alphaText = it }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    val r = redText.toIntOrNull()?.coerceIn(0, 255)
                    val g = greenText.toIntOrNull()?.coerceIn(0, 255)
                    val b = blueText.toIntOrNull()?.coerceIn(0, 255)
                    val a = alphaText.toIntOrNull()?.coerceIn(0, 255)
                    if (r == null || g == null || b == null || a == null) {
                        parseError = "RGBA 数值应在 0-255"
                    } else {
                        val updatedColor = Color((a shl 24) or (r shl 16) or (g shl 8) or b)
                        val updatedHsv = updatedColor.toHsvColor()
                        updateByHsv(updatedHsv.h, updatedHsv.s, updatedHsv.v, updatedHsv.a)
                    }
                }
            ) {
                Text("应用 RGBA")
            }
        }

        OutlinedTextField(
            value = parseInput,
            onValueChange = { parseInput = it },
            label = { Text("颜色输入") },
            placeholder = { Text("例如 #4F7CFF / rgb(79,124,255) / hsl(220,100%,65%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    val parsed = parseColorInput(parseInput)
                    if (parsed == null) {
                        parseError = "颜色格式无效，支持 HEX、RGB/RGBA、HSL/HSLA"
                    } else {
                        val parsedHsv = parsed.toHsvColor()
                        updateByHsv(parsedHsv.h, parsedHsv.s, parsedHsv.v, parsedHsv.a)
                        parseError = null
                    }
                }
            ) {
                Text("应用输入")
            }
        }

        if (parseError != null) {
            Text(
                text = parseError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ColorChannelField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.all { it.isDigit() }) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun updateByPointer(position: Offset) {
        if (size.width <= 0 || size.height <= 0) return
        val sat = (position.x / size.width).coerceIn(0f, 1f)
        val v = (1f - (position.y / size.height)).coerceIn(0f, 1f)
        onSaturationValueChange(sat, v)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .onSizeChanged { size = it }
            .pointerInput(hue) {
                detectTapGestures { offset -> updateByPointer(offset) }
            }
            .pointerInput(hue) {
                detectDragGestures(
                    onDragStart = { offset -> updateByPointer(offset) },
                    onDrag = { change, _ ->
                        updateByPointer(change.position)
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(Color.hsv(hue, 1f, 1f))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }

        if (size.width > 0 && size.height > 0) {
            val indicatorX = (saturation * size.width).roundToInt()
            val indicatorY = ((1f - value) * size.height).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX - 9, indicatorY - 9) }
                    .size(18.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.Transparent, CircleShape)
            )
        }
    }
}

private data class HsvColor(
    val h: Float,
    val s: Float,
    val v: Float,
    val a: Float
)

private fun defaultPaletteColor(isDarkMode: Boolean): Color {
    return if (isDarkMode) NightBlue else ElectricBlue
}

private fun Color.toHexString(): String {
    val argb = toArgb()
    val a = (argb ushr 24) and 0xFF
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return if (a == 255) {
        "#${r.toHex2()}${g.toHex2()}${b.toHex2()}"
    } else {
        "#${a.toHex2()}${r.toHex2()}${g.toHex2()}${b.toHex2()}"
    }
}

private fun Int.toHex2(): String = toString(16).uppercase().padStart(2, '0')

private fun String?.toColorOrNull(): Color? {
    if (this.isNullOrBlank()) return null
    val text = trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val normalized = when (text.length) {
        3 -> "FF" + text.map { "$it$it" }.joinToString("")
        4 -> text.map { "$it$it" }.joinToString("")
        6 -> "FF$text"
        8 -> text
        else -> return null
    }
    val value = normalized.toLongOrNull(16) ?: return null
    return Color(value.toInt())
}

private fun Color.toHsvColor(): HsvColor {
    val argb = toArgb()
    val r = ((argb ushr 16) and 0xFF) / 255f
    val g = ((argb ushr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val a = ((argb ushr 24) and 0xFF) / 255f

    val maxComponent = maxOf(r, g, b)
    val minComponent = minOf(r, g, b)
    val delta = maxComponent - minComponent

    val hue = when {
        delta == 0f -> 0f
        maxComponent == r -> (60f * ((g - b) / delta) + 360f) % 360f
        maxComponent == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val saturation = if (maxComponent == 0f) 0f else delta / maxComponent

    return HsvColor(hue, saturation, maxComponent, a)
}

private fun parseColorInput(input: String): Color? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.toColorOrNull()
        ?: parseRgbColor(trimmed)
        ?: parseHslColor(trimmed)
}

private fun parseRgbColor(input: String): Color? {
    val match =
        Regex("""^rgba?\((.+)\)$""", RegexOption.IGNORE_CASE).matchEntire(input) ?: return null
    val parts = match.groupValues[1].split(',').map { it.trim() }
    if (parts.size !in 3..4) return null

    val r = parseRgbChannel(parts[0]) ?: return null
    val g = parseRgbChannel(parts[1]) ?: return null
    val b = parseRgbChannel(parts[2]) ?: return null
    val a = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 1f

    return Color(
        red = r / 255f,
        green = g / 255f,
        blue = b / 255f,
        alpha = a
    )
}

private fun parseHslColor(input: String): Color? {
    val match =
        Regex("""^hsla?\((.+)\)$""", RegexOption.IGNORE_CASE).matchEntire(input) ?: return null
    val parts = match.groupValues[1].split(',').map { it.trim() }
    if (parts.size !in 3..4) return null

    val h = parts[0].removeSuffix("deg").toFloatOrNull() ?: return null
    val s = parsePercent(parts[1]) ?: return null
    val l = parsePercent(parts[2]) ?: return null
    val a = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 1f

    return hslToColor(h, s, l, a)
}

private fun parseRgbChannel(text: String): Int? {
    return if (text.endsWith("%")) {
        val percent = text.removeSuffix("%").toFloatOrNull() ?: return null
        ((percent.coerceIn(0f, 100f) / 100f) * 255f).roundToInt()
    } else {
        text.toIntOrNull()?.coerceIn(0, 255)
    }
}

private fun parseAlpha(text: String): Float? {
    return if (text.endsWith("%")) {
        val percent = text.removeSuffix("%").toFloatOrNull() ?: return null
        (percent / 100f).coerceIn(0f, 1f)
    } else {
        val value = text.toFloatOrNull() ?: return null
        if (value > 1f) (value / 255f).coerceIn(0f, 1f) else value.coerceIn(0f, 1f)
    }
}

private fun parsePercent(text: String): Float? {
    val normalized = text.removeSuffix("%").toFloatOrNull() ?: return null
    return (normalized / 100f).coerceIn(0f, 1f)
}

private fun hslToColor(hue: Float, saturation: Float, lightness: Float, alpha: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)

    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

@Composable
fun UserInfoCard(
    userInfo: com.suseoaa.projectoaa.shared.domain.model.person.PersonData?,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditInfo: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && userInfo != null) {
        EditInfoDialog(
            initialUsername = userInfo.username,
            initialName = userInfo.name,
            initialEmail = userInfo.email,
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
fun SettingGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            content()
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val content = @Composable {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showBadge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    SettingGroupCard(modifier = modifier) {
        SettingRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            showBadge = showBadge,
            trailingText = trailingText,
            trailingContent = trailingContent,
            onClick = onClick
        )
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
                        val currentTime =
                            com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
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
