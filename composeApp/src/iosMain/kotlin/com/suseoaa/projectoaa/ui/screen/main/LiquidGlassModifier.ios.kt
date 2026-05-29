package com.suseoaa.projectoaa.ui.screen.main

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.graphicsLayer

actual fun Modifier.liquidGlassDistortion(
    isExpanded: Boolean,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fallbackScaleX: Float,
    fallbackScaleY: Float,
    fallbackPivotX: Float,
    fallbackPivotY: Float
): Modifier = this.graphicsLayer {
    if (isExpanded) {
        scaleX = fallbackScaleX
        scaleY = fallbackScaleY
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
            pivotFractionX = fallbackPivotX,
            pivotFractionY = fallbackPivotY
        )
    }
}
