package com.suseoaa.projectoaa.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * iOS 平台 Toast 显示组件
 * 使用 Compose Popup 模拟原生 Toast 效果
 */
@Composable
actual fun ToastHandler() {
    var currentMessage by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(Unit) {
        ToastManager.toastMessages.collectLatest { toastMessage ->
            currentMessage = toastMessage
            val delayTime = when (toastMessage.duration) {
                ToastDuration.SHORT -> 2000L
                ToastDuration.LONG -> 3500L
            }
            delay(delayTime)
            currentMessage = null
        }
    }

    currentMessage?.let { message ->
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(focusable = false)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    .background(
                        color = Color(0xFF323232),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
