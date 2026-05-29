package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.ui.Modifier

/**
 * 跨平台液态玻璃折射畸变修饰符
 * 在 Android 13+ 会使用 AGSL 着色器产生真正的物理扭曲，其他平台提供优雅降级。
 */
expect fun Modifier.liquidGlassDistortion(
    isExpanded: Boolean,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fallbackScaleX: Float,
    fallbackScaleY: Float,
    fallbackPivotX: Float,
    fallbackPivotY: Float
): Modifier
