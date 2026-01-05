package com.suseoaa.projectoaa.core.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

// 卡片收缩动画
val AcademicSharedTransitionSpec: (Rect, Rect) -> FiniteAnimationSpec<Rect> =
    { _, _ ->
        tween(durationMillis = 400, easing = FastOutSlowInEasing)
    }