package com.suseoaa.projectoaa.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.presentation.person.PersonViewModel
import com.suseoaa.projectoaa.ui.screen.person.SettingCard
import com.suseoaa.projectoaa.ui.component.common.SharedTransitionPageContainer
import com.suseoaa.projectoaa.ui.theme.*
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartTabDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showLiquidGlassStyleDialog by remember { mutableStateOf(false) }

    SharedTransitionPageContainer(transitionKey = "settings") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 起始页设置
                item {
                    val startTabLabels = listOf("首页", "课程", "教务信息", "个人")
                    SettingCard(
                        icon = Icons.Default.Home,
                        title = "起始页设置",
                        subtitle = "打开应用时默认显示：${startTabLabels.getOrElse(uiState.defaultStartTab) { "首页" }}",
                        onClick = { showStartTabDialog = true }
                    )
                }

                // 预测性返回手势开关
                item {
                    SettingCard(
                        icon = Icons.Default.Edit,
                        title = "预测性返回手势",
                        subtitle = if (uiState.isPredictiveBackEnabled) "已开启，支持跟手滑动返回" else "已关闭",
                        trailingContent = {
                            Switch(
                                checked = uiState.isPredictiveBackEnabled,
                                onCheckedChange = { viewModel.togglePredictiveBackEnabled() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        },
                        onClick = null
                    )
                }

                // 液态玻璃导航栏开关
                item {
                    SettingCard(
                        icon = Icons.Default.Palette,
                        title = "液态玻璃导航栏",
                        subtitle = "开启后底部导航栏将呈现高斯模糊透明玻璃质感",
                        trailingContent = {
                            Switch(
                                checked = uiState.isLiquidGlassTabbarEnabled,
                                onCheckedChange = { viewModel.toggleLiquidGlassTabbarEnabled() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        },
                        onClick = null
                    )
                }

                if (uiState.isLiquidGlassTabbarEnabled) {
                    item {
                        SettingCard(
                            icon = Icons.Default.Palette,
                            title = "液态玻璃样式",
                            subtitle = if (uiState.liquidGlassTabbarStyle == 1) "当前：液态薄片·杂" else "当前：液态玻璃·纯",
                            onClick = { showLiquidGlassStyleDialog = true }
                        )
                    }
                }

                // 莫奈取色开关 (Dynamic Color)
                item {
                    SettingCard(
                        icon = Icons.Default.Edit,
                        title = "动态取色",
                        subtitle = if (uiState.isDynamicColorEnabled) "已开启，可使用下方莫奈调色盘自定义主题强调色" else "已关闭，当前使用软件默认配色",
                        trailingContent = {
                            Switch(
                                checked = uiState.isDynamicColorEnabled,
                                onCheckedChange = { viewModel.toggleDynamicColor() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        },
                        onClick = null
                    )
                }

                if (uiState.isDynamicColorEnabled) {
                    item {
                        DynamicColorPaletteEntryCard(
                            lightColorHex = uiState.dynamicPaletteLightColorHex,
                            darkColorHex = uiState.dynamicPaletteDarkColorHex,
                            dynamicColorEnabled = uiState.isDynamicColorEnabled,
                            onClick = { showPaletteDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showStartTabDialog) {
        StartTabDialog(
            currentTab = uiState.defaultStartTab,
            onDismiss = { showStartTabDialog = false },
            onConfirm = {
                viewModel.saveDefaultStartTab(it) // It was saveDefaultStartTab in PersonScreen
                showStartTabDialog = false
            }
        )
    }

    if (showPaletteDialog) {
        DynamicColorPaletteDialog(
            currentLightColorHex = uiState.dynamicPaletteLightColorHex,
            currentDarkColorHex = uiState.dynamicPaletteDarkColorHex,
            onDismiss = { showPaletteDialog = false },
            onConfirm = { light, dark ->
                viewModel.setDynamicPaletteColors(light, dark)
                showPaletteDialog = false
            }
        )
    }

    if (showLiquidGlassStyleDialog) {
        LiquidGlassStyleDialog(
            currentStyle = uiState.liquidGlassTabbarStyle,
            onDismiss = { showLiquidGlassStyleDialog = false },
            onConfirm = { style ->
                viewModel.setLiquidGlassTabbarStyle(style)
                showLiquidGlassStyleDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 从个人界面移动过来的 UI 组件
// -------------------------------------------------------------

@Composable
fun DynamicColorPaletteEntryCard(
    lightColorHex: String?,
    darkColorHex: String?,
    dynamicColorEnabled: Boolean,
    onClick: () -> Unit
) {
    val lightColor = lightColorHex?.toColorOrNull() ?: defaultPaletteColor(isDarkMode = false)
    val darkColor = darkColorHex?.toColorOrNull() ?: defaultPaletteColor(isDarkMode = true)
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

@Composable
fun StartTabDialog(
    currentTab: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val tabOptions = listOf("首页", "课程", "教务信息", "个人")
    var selectedTab by remember { mutableIntStateOf(currentTab) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("起始页设置", style = MaterialTheme.typography.titleMedium) },
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.4f
                        ),
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
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedTab == index) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
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
        confirmButton = { Button(onClick = { onConfirm(selectedTab) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

data class DynamicColorPaletteOption(val label: String, val color: Color)

@Composable
fun LiquidGlassStyleDialog(
    currentStyle: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = mapOf(1 to "液态薄片·杂", 2 to "液态玻璃·纯")
    var selectedStyle by remember { mutableIntStateOf(currentStyle) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("液态玻璃样式", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(
                    "选择液态玻璃导航栏的显示样式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                options.forEach { (styleValue, label) ->
                    Surface(
                        onClick = { selectedStyle = styleValue },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedStyle == styleValue) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.4f
                        ),
                        tonalElevation = if (selectedStyle == styleValue) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedStyle == styleValue) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedStyle == styleValue) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedStyle == styleValue) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
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
        confirmButton = { Button(onClick = { onConfirm(selectedStyle) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

val DynamicColorPaletteOptions = listOf(
    DynamicColorPaletteOption("电光蓝", ElectricBlue),
    DynamicColorPaletteOption("薄荷青", Color(0xFF00BFA5)),
    DynamicColorPaletteOption("天青", Color(0xFF00BCD4)),
    DynamicColorPaletteOption("湖蓝", Color(0xFF42A5F5)),
    DynamicColorPaletteOption("深海", Color(0xFF1565C0)),
    DynamicColorPaletteOption("夜蓝", NightBlue),
    DynamicColorPaletteOption("深紫", Color(0xFF4527A0)),
    DynamicColorPaletteOption("浅紫", Color(0xFF7E57C2)),
    DynamicColorPaletteOption("紫红", Color(0xFFC2185B)),
    DynamicColorPaletteOption("胭脂", Color(0xFFE91E63)),
    DynamicColorPaletteOption("夕阳", Color(0xFFFF7043)),
    DynamicColorPaletteOption("金橙", Color(0xFFFFA000)),
    DynamicColorPaletteOption("秋叶", Color(0xFFFBC02D)),
    DynamicColorPaletteOption("青柠", Color(0xFFAFEEE)),
    DynamicColorPaletteOption("松绿", Color(0xFF00695C)),
    DynamicColorPaletteOption("森林", Color(0xFF2E7D32)),
    DynamicColorPaletteOption("墨绿", Color(0xFF1B5E20)),
    DynamicColorPaletteOption("极光绿", Color(0xFF00C853)),
    DynamicColorPaletteOption("赤土", Color(0xFFBF360C)),
    DynamicColorPaletteOption("咖啡", Color(0xFF4E342E)),
    DynamicColorPaletteOption("灰蓝", Color(0xFF37474F)),
    DynamicColorPaletteOption("石板", Color(0xFF455A64))
)

fun defaultPaletteColor(isDarkMode: Boolean): Color {
    return if (isDarkMode) ElectricBlue else ElectricBlue
}

fun String.toColorOrNull(): Color? {
    if (!startsWith("#") || (length != 7 && length != 9)) return null
    return try {
        val colorInt = removePrefix("#").toLong(16)
        val alpha = if (length == 9) (colorInt shr 24).toFloat() / 255f else 1f
        val red = ((colorInt shr 16) and 0xFF).toFloat() / 255f
        val green = ((colorInt shr 8) and 0xFF).toFloat() / 255f
        val blue = (colorInt and 0xFF).toFloat() / 255f
        Color(red = red, green = green, blue = blue, alpha = alpha)
    } catch (e: Exception) {
        null
    }
}

fun Color.toHexString(): String {
    val alphaInt = (this.alpha * 255).toInt()
    val redInt = (this.red * 255).toInt()
    val greenInt = (this.green * 255).toInt()
    val blueInt = (this.blue * 255).toInt()

    fun Int.toHex2(): String = this.toString(16).padStart(2, '0').uppercase()

    return if (alphaInt == 255) {
        "#${redInt.toHex2()}${greenInt.toHex2()}${blueInt.toHex2()}"
    } else {
        "#${alphaInt.toHex2()}${redInt.toHex2()}${greenInt.toHex2()}${blueInt.toHex2()}"
    }
}

@Composable
fun DynamicColorPaletteDialog(
    currentLightColorHex: String?,
    currentDarkColorHex: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var lightColorHex by remember { mutableStateOf(currentLightColorHex) }
    var darkColorHex by remember { mutableStateOf(currentDarkColorHex) }
    var isEditingLightColor by remember { mutableStateOf(true) }
    var isCustomMode by remember { mutableStateOf(false) }
    var customHue by remember { mutableFloatStateOf(210f) }
    var customSaturation by remember { mutableFloatStateOf(0.8f) }
    var customLightness by remember { mutableFloatStateOf(0.5f) }

    val activeColorHex = if (isEditingLightColor) lightColorHex else darkColorHex
    val defaultColor = defaultPaletteColor(!isEditingLightColor)
    val activeColor = activeColorHex?.toColorOrNull() ?: defaultColor

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 头部
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp)
                ) {
                    Text(
                        "莫奈调色盘",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "自定义亮色与暗色模式的强调色",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 浅色模式目标
                        Surface(
                            onClick = { isEditingLightColor = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEditingLightColor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isEditingLightColor) BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
                            ) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "亮色模式",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isEditingLightColor) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(
                                        lightColorHex?.toColorOrNull() ?: defaultPaletteColor(false)
                                    ).border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    lightColorHex ?: "自动",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 暗色模式目标
                        Surface(
                            onClick = { isEditingLightColor = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isEditingLightColor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (!isEditingLightColor) BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
                            ) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "暗色模式",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (!isEditingLightColor) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(
                                        darkColorHex?.toColorOrNull() ?: defaultPaletteColor(true)
                                    ).border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    darkColorHex ?: "自动",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 内容区
                Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "选择颜色",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "预设",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCustomMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            )
                            Switch(
                                checked = isCustomMode,
                                onCheckedChange = { isCustomMode = it },
                                modifier = Modifier.padding(horizontal = 8.dp)
                                    .size(width = 40.dp, height = 24.dp)
                            )
                            Text(
                                "自定义",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCustomMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    if (isCustomMode) {
                        // 自定义颜色选择器（针对设置界面简化）
                        val previewColor =
                            hslToColor(customHue, customSaturation, customLightness, 1f)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                                .clip(RoundedCornerShape(16.dp)).background(previewColor)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("色相 (Hue)")
                        Slider(
                            value = customHue,
                            onValueChange = { customHue = it },
                            valueRange = 0f..360f
                        )
                        Text("饱和度 (Saturation)")
                        Slider(
                            value = customSaturation,
                            onValueChange = { customSaturation = it },
                            valueRange = 0f..1f
                        )
                        Text("亮度 (Lightness)")
                        Slider(
                            value = customLightness,
                            onValueChange = { customLightness = it },
                            valueRange = 0f..1f
                        )
                        Button(onClick = {
                            if (isEditingLightColor) lightColorHex = previewColor.toHexString()
                            else darkColorHex = previewColor.toHexString()
                        }) { Text("应用") }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (isEditingLightColor) lightColorHex =
                                            null else darkColorHex = null
                                    }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            ), contentAlignment = Alignment.Center
                                    ) {
                                        if (activeColorHex == null) Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        "自动 (随系统莫奈或默认)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            items(DynamicColorPaletteOptions.size) { index ->
                                val option = DynamicColorPaletteOptions[index]
                                val isSelected = activeColorHex?.toColorOrNull() == option.color
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (isEditingLightColor) lightColorHex =
                                            option.color.toHexString()
                                        else darkColorHex = option.color.toHexString()
                                    }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(option.color).border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            ), contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (option.color.luminance() > 0.5f) Color.Black else Color.White
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }

                // 底部
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(lightColorHex, darkColorHex) }) { Text("保存") }
                }
            }
        }
    }
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

fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055) / 1.055).pow(2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055) / 1.055).pow(2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055) / 1.055).pow(2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
