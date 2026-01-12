package com.suseoaa.projectoaa.core.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

fun <T> containerVisualPhysics(): FiniteAnimationSpec<T> = tween(
    durationMillis = 500,
    easing = FastOutSlowInEasing
)

fun <T> keepAlivePhysics(): FiniteAnimationSpec<T> = tween(
    durationMillis = 750,
    easing = LinearEasing
)

val AcademicSharedTransitionSpec: (Rect, Rect) -> FiniteAnimationSpec<Rect> =
    { _, _ ->
        tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    }