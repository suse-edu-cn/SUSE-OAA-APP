package com.suseoaa.projectoaa.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ================== 标准 Material Design 颜色 ==================
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ================== [新] 二次元风格 (Anime Palette) ==================
val AnimePinkPrimary = Color(0xFFFF9EAC) // 樱花粉
val AnimePinkOnPrimary = Color(0xFFFFFFFF)
val AnimePinkContainer = Color(0xFFFFDCE0)

val AnimeBlueSecondary = Color(0xFF8FD3F4) // 天空蓝
val AnimeBlueOnSecondary = Color(0xFFFFFFFF)
val AnimeBlueContainer = Color(0xFFD0F0FD)

val AnimeBackgroundLight = Color(0xFFFFF8F9) // 极淡粉白
val AnimeSurfaceLight = Color(0xFFFFFFFF)
val AnimeOutline = Color(0xFFFFC1CC)

// ================== [新] Android 2.3 Gingerbread 风格 ==================
val GingerOrange = Color(0xFFFF8800)
val GingerGreen = Color(0xFF99CC00)
val GingerBackground = Color(0xFF101010)
val GingerSurface = Color(0xFF202020)
val GingerText = Color(0xFFEBEBEB)

// ================== [新] Android 4.x Holo 风格 ==================
val HoloBlue = Color(0xFF33B5E5)
val HoloDarkBg = Color(0xFF000000)
val HoloSurface = Color(0xFF222222)
val HoloContent = Color(0xFFFFFFFF)

// ================== 全局形状定义 ==================
val AnimeShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)