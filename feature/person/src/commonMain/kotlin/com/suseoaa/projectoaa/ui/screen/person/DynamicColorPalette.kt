package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt
import com.suseoaa.projectoaa.ui.component.SettingCard

// 动态取色的入口卡片、选色对话框与取色器控件。

internal val DynamicColorPaletteOptions = listOf(
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

@Composable
internal fun DynamicColorPaletteEntryCard(
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
internal fun DynamicColorPaletteDialog(
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
internal fun DynamicColorPaletteEditor(
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
internal fun ColorChannelField(
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
internal fun SaturationValuePicker(
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

/** 预设配色项。 */
internal data class DynamicColorPaletteOption(
    val label: String,
    val color: Color
)
