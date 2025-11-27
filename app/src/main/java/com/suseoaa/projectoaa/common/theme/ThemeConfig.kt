package com.suseoaa.projectoaa.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 主题配置接口
 */
data class OaaThemeConfig(
    val name: String,
    val colorScheme: ColorScheme,
    val shapes: Shapes,
    val appBackground: Color? = null,
    val isDark: Boolean = false
)

/**
 * 全局主题管理器
 */
object ThemeManager {
    // 修改 1: 默认主题改为 MaterialDesignTheme
    var currentTheme by mutableStateOf(MaterialDesignTheme)

    // 修改 2: 调整列表顺序，将 Material Design 放在首位
    val themeList = listOf(
        MaterialDesignTheme, // 首位：标准 MD
        AnimeLightTheme,     // 第二：二次元 (后台会自动缓存壁纸)
        HoloDarkTheme,       // 第三：复古 4.0
        GingerbreadTheme     // 第四：复古 2.3
    )
}

// ================== 1. 标准 Material Design (移动到最前，方便查找) ==================
val MaterialDesignTheme = OaaThemeConfig(
    name = "Material Design (Standard)",
    colorScheme = lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40
    ),
    shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(8.dp)
    ),
    appBackground = Color(0xFFFFFBFE),
    isDark = false
)

// ================== 2. 二次元主题 (Sakura) ==================
val AnimeLightTheme = OaaThemeConfig(
    name = "二次元 (Sakura)",
    colorScheme = lightColorScheme(
        primary = AnimePinkPrimary,
        onPrimary = AnimePinkOnPrimary,
        primaryContainer = AnimePinkContainer,
        secondary = AnimeBlueSecondary,
        onSecondary = AnimeBlueOnSecondary,
        secondaryContainer = AnimeBlueContainer,
        background = AnimeBackgroundLight,
        surface = AnimeSurfaceLight,
        outline = AnimeOutline
    ),
    shapes = AnimeShapes,
    appBackground = AnimeBackgroundLight,
    isDark = false
)

// ================== 3. Android 4.x Holo Dark ==================
val HoloDarkTheme = OaaThemeConfig(
    name = "Android 4.0 (Holo Dark)",
    colorScheme = darkColorScheme(
        primary = HoloBlue,
        onPrimary = Color.Black,
        primaryContainer = Color.Black,
        secondary = HoloBlue,
        background = HoloDarkBg,
        surface = HoloSurface,
        onSurface = HoloContent,
        outline = Color.Gray
    ),
    shapes = Shapes(
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp)
    ),
    appBackground = HoloDarkBg,
    isDark = true
)

// ================== 4. Android 2.3 Gingerbread ==================
val GingerbreadTheme = OaaThemeConfig(
    name = "Android 2.3 (Gingerbread)",
    colorScheme = darkColorScheme(
        primary = GingerOrange,
        onPrimary = Color.Black,
        primaryContainer = GingerSurface,
        secondary = GingerGreen,
        background = GingerBackground,
        surface = GingerSurface,
        onSurface = GingerText,
        outline = GingerOrange
    ),
    shapes = Shapes(
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp)
    ),
    appBackground = GingerBackground,
    isDark = true
)