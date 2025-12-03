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

data class OaaThemeConfig(
    val name: String,
    val colorScheme: ColorScheme,
    val shapes: Shapes,
    val appBackground: Color? = null,
    val isDark: Boolean = false
)

object ThemeManager {
    // 默认使用升级后的高级白主题
    var currentTheme by mutableStateOf(MinimalistTheme)

    val themeList = listOf(
        MinimalistTheme,
        MaterialDesignTheme,
        AnimeLightTheme,
        HoloDarkTheme,
        GingerbreadTheme
    )
}

// ================== [升级] 高级白 (Premium White) ==================
val MinimalistTheme = OaaThemeConfig(
    name = "高级白 (Premium)",
    colorScheme = lightColorScheme(
        primary = PremiumPrimary,
        onPrimary = PremiumOnPrimary,

        // 选中状态的容器色 (例如选中的 Tab)
        primaryContainer = PremiumPrimaryContainer,
        onPrimaryContainer = PremiumOnPrimaryContainer,

        secondary = PremiumSecondary,
        onSecondary = Color.White,

        tertiary = PremiumTertiary, // 引入第三色增加活力

        background = PremiumBackground, // 灰白背景
        onBackground = PremiumTextMain,

        surface = PremiumSurface,       // 纯白卡片
        onSurface = PremiumTextMain,

        surfaceVariant = PremiumSurface, // 也是纯白，用于某些变体卡片
        onSurfaceVariant = PremiumTextSub,

        outline = PremiumOutline,
        outlineVariant = Color(0xFFEEEEEE) // 极淡的分隔线
    ),
    shapes = Shapes(
        small = RoundedCornerShape(12.dp),  // 更圆润，符合现代高级感
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp)
    ),
    appBackground = PremiumBackground,
    isDark = false
)

// ... 其他主题保持不变 ...
val MaterialDesignTheme = OaaThemeConfig(
    name = "Material Design (Standard)",
    colorScheme = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40),
    shapes = Shapes(small = RoundedCornerShape(4.dp), medium = RoundedCornerShape(4.dp), large = RoundedCornerShape(8.dp)),
    appBackground = Color(0xFFFFFBFE),
    isDark = false
)

val AnimeLightTheme = OaaThemeConfig(
    name = "二次元 (Sakura)",
    colorScheme = lightColorScheme(
        primary = AnimePinkPrimary, onPrimary = AnimePinkOnPrimary, primaryContainer = AnimePinkContainer,
        secondary = AnimeBlueSecondary, onSecondary = AnimeBlueOnSecondary, secondaryContainer = AnimeBlueContainer,
        background = AnimeBackgroundLight, surface = AnimeSurfaceLight, outline = AnimeOutline
    ),
    shapes = AnimeShapes,
    appBackground = AnimeBackgroundLight,
    isDark = false
)

val HoloDarkTheme = OaaThemeConfig(
    name = "Android 4.0 (Holo Dark)",
    colorScheme = darkColorScheme(
        primary = HoloBlue, onPrimary = Color.Black, primaryContainer = Color.Black,
        secondary = HoloBlue, background = HoloDarkBg, surface = HoloSurface, onSurface = HoloContent, outline = Color.Gray
    ),
    shapes = Shapes(small = RoundedCornerShape(0.dp), medium = RoundedCornerShape(0.dp), large = RoundedCornerShape(0.dp)),
    appBackground = HoloDarkBg,
    isDark = true
)

val GingerbreadTheme = OaaThemeConfig(
    name = "Android 2.3 (Gingerbread)",
    colorScheme = darkColorScheme(
        primary = GingerOrange, onPrimary = Color.Black, primaryContainer = GingerSurface,
        secondary = GingerGreen, background = GingerBackground, surface = GingerSurface, onSurface = GingerText, outline = GingerOrange
    ),
    shapes = Shapes(small = RoundedCornerShape(0.dp), medium = RoundedCornerShape(0.dp), large = RoundedCornerShape(0.dp)),
    appBackground = GingerBackground,
    isDark = true
)