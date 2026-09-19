package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.suseoaa.projectoaa.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

// 颜色文本的解析与转换。
//
// 用户可以直接输入 #RRGGBB / rgb(...) / hsl(...) 指定主题色。这些是纯函数，
// 原本混在「我的」页面的 UI 文件里无从验证，拆出来后由 ColorParsingTest 覆盖
// 各种格式与非法输入。

internal data class HsvColor(
    val h: Float,
    val s: Float,
    val v: Float,
    val a: Float
)

internal fun defaultPaletteColor(isDarkMode: Boolean): Color {
    return if (isDarkMode) NightBlue else ElectricBlue
}

internal fun Color.toHexString(): String {
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

internal fun Int.toHex2(): String = toString(16).uppercase().padStart(2, '0')

internal fun String?.toColorOrNull(): Color? {
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

internal fun Color.toHsvColor(): HsvColor {
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

internal fun parseColorInput(input: String): Color? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.toColorOrNull()
        ?: parseRgbColor(trimmed)
        ?: parseHslColor(trimmed)
}

internal fun parseRgbColor(input: String): Color? {
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

internal fun parseHslColor(input: String): Color? {
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

internal fun parseRgbChannel(text: String): Int? {
    return if (text.endsWith("%")) {
        val percent = text.removeSuffix("%").toFloatOrNull() ?: return null
        ((percent.coerceIn(0f, 100f) / 100f) * 255f).roundToInt()
    } else {
        text.toIntOrNull()?.coerceIn(0, 255)
    }
}

internal fun parseAlpha(text: String): Float? {
    return if (text.endsWith("%")) {
        val percent = text.removeSuffix("%").toFloatOrNull() ?: return null
        (percent / 100f).coerceIn(0f, 1f)
    } else {
        val value = text.toFloatOrNull() ?: return null
        if (value > 1f) (value / 255f).coerceIn(0f, 1f) else value.coerceIn(0f, 1f)
    }
}

internal fun parsePercent(text: String): Float? {
    val normalized = text.removeSuffix("%").toFloatOrNull() ?: return null
    return (normalized / 100f).coerceIn(0f, 1f)
}

internal fun hslToColor(hue: Float, saturation: Float, lightness: Float, alpha: Float): Color {
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
