package com.suseoaa.projectoaa.util

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

/**
 * Android 平台 Toast 显示组件
 * 在 Compose 中收集 ToastManager 的消息并显示原生 Toast
 */
@Composable
actual fun ToastHandler() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ToastManager.toastMessages.collectLatest { toastMessage ->
            val duration = when (toastMessage.duration) {
                ToastDuration.SHORT -> Toast.LENGTH_SHORT
                ToastDuration.LONG -> Toast.LENGTH_LONG
            }
            Toast.makeText(context, toastMessage.message, duration).show()
        }
    }
}
