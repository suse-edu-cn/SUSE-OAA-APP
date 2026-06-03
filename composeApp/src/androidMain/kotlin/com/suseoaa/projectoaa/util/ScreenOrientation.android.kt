package com.suseoaa.projectoaa.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun LockScreenOrientation(landscape: Boolean) {
    val context = LocalContext.current
    DisposableEffect(landscape) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        onDispose {
            // Restore original orientation
            activity.requestedOrientation = originalOrientation
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
