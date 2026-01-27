package com.suseoaa.projectoaa.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== iOS 风格配色方案 ====================

// 亮色模式
val OxygenWhite = Color(0xFFFFFFFF)        // 纯净白：卡片、BottomBar
val OxygenBackground = Color(0xFFF2F4F6)   // 云朵灰白：模仿 iOS/MIUI 的系统底色 (通透感来源)
val ElectricBlue = Color(0xFF007AFF)       // 电光蓝：模仿 iOS 系统蓝，高饱和，非常有活力
val SoftBlueWait = Color(0xFFE3F2FD)       // 浅云蓝：选中状态的背景 (果冻感)

// 文字颜色
val InkBlack = Color(0xFF191C1E)           // 主要文字
val InkGrey = Color(0xFF70767E)            // 次要文字 (未选中图标)
val OutlineSoft = Color(0xFFE8E9EB)        // 极其淡的分割线

// 暗色模式
val NightBackground = Color(0xFF000000)    // 纯黑
val NightSurface = Color(0xFF1C1C1E)       // 浮层灰 (iOS 风格)
val NightBlue = Color(0xFF0A84FF)          // 夜间模式的亮蓝
val NightContainer = Color(0xFF002F58)     // 夜间选中背景

// 功能色
val AlertRed = Color(0xFFFF3B30)           // iOS 风格红

// ==================== Material 3 兼容配色 ====================

// Light Theme Colors (保留兼容性)
val md_theme_light_primary = ElectricBlue
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = SoftBlueWait
val md_theme_light_onPrimaryContainer = ElectricBlue
val md_theme_light_secondary = ElectricBlue
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = SoftBlueWait
val md_theme_light_onSecondaryContainer = ElectricBlue
val md_theme_light_tertiary = Color(0xFF7E5260)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFD9E3)
val md_theme_light_onTertiaryContainer = Color(0xFF31101D)
val md_theme_light_error = AlertRed
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = OxygenBackground
val md_theme_light_onBackground = InkBlack
val md_theme_light_surface = OxygenWhite
val md_theme_light_onSurface = InkBlack
val md_theme_light_surfaceVariant = OxygenBackground
val md_theme_light_onSurfaceVariant = InkGrey
val md_theme_light_outline = OutlineSoft
val md_theme_light_outlineVariant = Color(0xFFC7C7CC)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inversePrimary = NightBlue

// Dark Theme Colors
val md_theme_dark_primary = NightBlue
val md_theme_dark_onPrimary = Color(0xFFFFFFFF)
val md_theme_dark_primaryContainer = NightContainer
val md_theme_dark_onPrimaryContainer = Color(0xFFFFFFFF)
val md_theme_dark_secondary = NightBlue
val md_theme_dark_onSecondary = Color(0xFFFFFFFF)
val md_theme_dark_secondaryContainer = NightContainer
val md_theme_dark_onSecondaryContainer = Color(0xFFFFFFFF)
val md_theme_dark_tertiary = Color(0xFFEFB8C8)
val md_theme_dark_onTertiary = Color(0xFF4A2532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD9E3)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = NightBackground
val md_theme_dark_onBackground = Color(0xFFFFFFFF)
val md_theme_dark_surface = NightSurface
val md_theme_dark_onSurface = Color(0xFFFFFFFF)
val md_theme_dark_surfaceVariant = NightSurface
val md_theme_dark_onSurfaceVariant = Color(0xFF8E8E93)
val md_theme_dark_outline = Color(0xFF2C2C2E)
val md_theme_dark_inverseOnSurface = NightBackground
val md_theme_dark_inverseSurface = Color(0xFFE6E1E6)
val md_theme_dark_inversePrimary = ElectricBlue
