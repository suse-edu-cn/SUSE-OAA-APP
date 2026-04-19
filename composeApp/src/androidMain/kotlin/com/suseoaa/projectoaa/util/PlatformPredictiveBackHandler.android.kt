package com.suseoaa.projectoaa.util

import android.util.Log
import android.provider.Settings
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val PREDICTIVE_BACK_TAG = "PredictiveBack"

@Composable
actual fun rememberPlatformNavigationMode(): PlatformNavigationMode {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val leftGestureInset = WindowInsets.systemGestures.getLeft(
        density = density,
        layoutDirection = layoutDirection
    )
    val rightGestureInset = WindowInsets.systemGestures.getRight(
        density = density,
        layoutDirection = layoutDirection
    )

    val settingValue = runCatching {
        Settings.Secure.getInt(context.contentResolver, "navigation_mode")
    }.getOrNull()

    return when {
        settingValue == 2 -> PlatformNavigationMode.Gesture
        settingValue != null -> PlatformNavigationMode.ThreeButton
        leftGestureInset > 0 || rightGestureInset > 0 -> PlatformNavigationMode.Gesture
        else -> PlatformNavigationMode.ThreeButton
    }
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (PlatformPredictiveBackEvent) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val currentOnBack by rememberUpdatedState(onBack)
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    DisposableEffect(dispatcher, lifecycleOwner, enabled, screenWidthPx, screenHeightPx) {
        if (dispatcher == null) {
            onDispose { }
        } else {
            val callback = object : OnBackPressedCallback(enabled) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    val distanceProgress = backEvent.toScreenDistanceProgress(screenWidthPx)
                    val verticalPosition = backEvent.toScreenVerticalPosition(screenHeightPx)
                    Log.d(
                        PREDICTIVE_BACK_TAG,
                        "started progress=${backEvent.progress} distance=${distanceProgress} vertical=${verticalPosition} edge=${backEvent.swipeEdge}"
                    )
                    currentOnProgress(
                        PlatformPredictiveBackEvent(
                            progress = backEvent.progress.coerceIn(0f, 1f),
                            swipeEdge = backEvent.toPlatformSwipeEdge(),
                            distanceProgress = distanceProgress,
                            verticalPosition = verticalPosition
                        )
                    )
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    val distanceProgress = backEvent.toScreenDistanceProgress(screenWidthPx)
                    val verticalPosition = backEvent.toScreenVerticalPosition(screenHeightPx)
                    Log.d(
                        PREDICTIVE_BACK_TAG,
                        "progressed progress=${backEvent.progress} distance=${distanceProgress} vertical=${verticalPosition} edge=${backEvent.swipeEdge}"
                    )
                    currentOnProgress(
                        PlatformPredictiveBackEvent(
                            progress = backEvent.progress.coerceIn(0f, 1f),
                            swipeEdge = backEvent.toPlatformSwipeEdge(),
                            distanceProgress = distanceProgress,
                            verticalPosition = verticalPosition
                        )
                    )
                }

                override fun handleOnBackCancelled() {
                    Log.d(PREDICTIVE_BACK_TAG, "cancelled")
                    currentOnCancel()
                }

                override fun handleOnBackPressed() {
                    Log.d(PREDICTIVE_BACK_TAG, "pressed")
                    currentOnBack()
                }
            }

            dispatcher.addCallback(lifecycleOwner, callback)

            onDispose {
                callback.remove()
            }
        }
    }
}

private fun BackEventCompat.toPlatformSwipeEdge(): PlatformBackSwipeEdge {
    return if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
        PlatformBackSwipeEdge.Right
    } else {
        PlatformBackSwipeEdge.Left
    }
}

private fun BackEventCompat.toScreenDistanceProgress(screenWidthPx: Float): Float {
    if (screenWidthPx <= 0f) return progress.coerceIn(0f, 1f)

    val distancePx = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
        (screenWidthPx - touchX).coerceAtLeast(0f)
    } else {
        touchX.coerceAtLeast(0f)
    }

    return (distancePx / screenWidthPx).coerceIn(0f, 1f)
}

private fun BackEventCompat.toScreenVerticalPosition(screenHeightPx: Float): Float {
    if (screenHeightPx <= 0f) return 0.5f
    return (touchY / screenHeightPx).coerceIn(0f, 1f)
}
