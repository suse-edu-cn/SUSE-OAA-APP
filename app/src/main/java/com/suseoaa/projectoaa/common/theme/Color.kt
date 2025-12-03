package com.suseoaa.projectoaa.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ... 保持原有 Material/Anime/Retro 颜色不变，防止其他主题报错 ...
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val AnimePinkPrimary = Color(0xFFFF9EAC)
val AnimePinkOnPrimary = Color(0xFFFFFFFF)
val AnimePinkContainer = Color(0xFFFFDCE0)
val AnimeBlueSecondary = Color(0xFF8FD3F4)
val AnimeBlueOnSecondary = Color(0xFFFFFFFF)
val AnimeBlueContainer = Color(0xFFD0F0FD)
val AnimeBackgroundLight = Color(0xFFFFF8F9)
val AnimeSurfaceLight = Color(0xFFFFFFFF)
val AnimeOutline = Color(0xFFFFC1CC)

// ================== [新] 高级白 + 活力点缀 (Premium Lively White) ==================

// 1. 活泼的主色：活力蓝 (类似于 iOS 的蓝或 Material 3 的 Primary)
// 这种蓝色在白色背景上非常醒目且高级
val PremiumPrimary = Color(0xFF2979FF)      // 活力蓝：用于按钮、重要图标
val PremiumOnPrimary = Color(0xFFFFFFFF)    // 蓝底白字

// 2. 更有层次的背景
val PremiumBackground = Color(0xFFF5F7FA)   // 高级灰白背景：带一点点冷色调，显得通透
val PremiumSurface = Color(0xFFFFFFFF)      // 纯白卡片：放在灰白背景上，通过阴影区分

// 3. 辅助色
val PremiumSecondary = Color(0xFF00B0FF)    // 浅一点的蓝，用于次级点缀
val PremiumTertiary = Color(0xFFFF4081)     // 玫红色：用于极少量的强调（如红点、热点），增加活泼感

// 4. 文字颜色：不要纯黑 (#000000)，太硬
val PremiumTextMain = Color(0xFF1A1C1E)     // 极深灰
val PremiumTextSub = Color(0xFF757575)      // 次要文字

// 5. 容器与边框
val PremiumPrimaryContainer = Color(0xFFE3F2FD) // 极淡的蓝色背景，用于选中项
val PremiumOnPrimaryContainer = Color(0xFF1565C0) // 深蓝文字
val PremiumOutline = Color(0xFFE0E0E0)

// ================== 全局形状定义 ==================
val AnimeShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// ... 复古颜色保持不变 ...
val GingerOrange = Color(0xFFFF8800)
val GingerGreen = Color(0xFF99CC00)
val GingerBackground = Color(0xFF101010)
val GingerSurface = Color(0xFF202020)
val GingerText = Color(0xFFEBEBEB)

val HoloBlue = Color(0xFF33B5E5)
val HoloDarkBg = Color(0xFF000000)
val HoloSurface = Color(0xFF222222)
val HoloContent = Color(0xFFFFFFFF)