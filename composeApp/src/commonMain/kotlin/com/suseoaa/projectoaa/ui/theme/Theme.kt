package com.suseoaa.projectoaa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 亮色模式 - iOS 风格
private val LightColorScheme = lightColorScheme(
    // 主色组 核心交互元素 (按钮等)
    primary = ElectricBlue,            // 电光蓝 (#007AFF)
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = SoftBlueWait,   // 浅蓝容器
    onPrimaryContainer = ElectricBlue,

    // 次要色组 BottomBar 选中状态依赖这里！
    secondary = ElectricBlue,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = SoftBlueWait, // 选中背景：浅云蓝 (#E3F2FD)
    onSecondaryContainer = ElectricBlue, // 选中图标：电光蓝

    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,

    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,

    // 背景与表面
    background = OxygenBackground,     // App 背景：云朵灰白 (#F2F4F6)
    onBackground = InkBlack,           // 文字：墨黑
    surface = OxygenWhite,             // 卡片/BottomBar：纯白 (#FFFFFF)
    onSurface = InkBlack,              // 卡片文字

    // 未选中状态
    surfaceVariant = OxygenBackground,
    onSurfaceVariant = InkGrey,        // 未选中图标：柔和灰 (#70767E)

    // 边框
    outline = OutlineSoft,             // 极淡边框
    outlineVariant = md_theme_light_outlineVariant,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

// 暗色模式 - iOS 风格
private val DarkColorScheme = darkColorScheme(
    primary = NightBlue,               // 亮蓝
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = NightContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,

    // BottomBar 选中态
    secondary = NightBlue,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = NightContainer, // 深蓝背景
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,  // 白色图标 (高亮)

    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,

    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,

    background = NightBackground,      // 纯黑
    onBackground = md_theme_dark_onBackground,
    surface = NightSurface,            // 深灰卡片
    onSurface = md_theme_dark_onSurface,

    // 未选中状态
    surfaceVariant = NightSurface,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant, // 浅灰图标

    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
)

// Typography
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ProjectOAATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
