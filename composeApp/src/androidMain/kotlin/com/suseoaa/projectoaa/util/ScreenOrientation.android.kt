package com.suseoaa.projectoaa.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
            // 恢复原始方向
            activity.requestedOrientation = originalOrientation
        }
    }
}

@Composable
actual fun LockFullscreen(fullscreen: Boolean) {
    val context = LocalContext.current
    DisposableEffect(fullscreen) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        
        if (fullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
