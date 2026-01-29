package com.suseoaa.projectoaa.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * iOS 平台 Toast 显示组件
 * 使用 Compose 实现美观优雅的 Toast 效果
 */
@Composable
actual fun ToastHandler() {
    var currentMessage by remember { mutableStateOf<ToastMessage?>(null) }
    var visible by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        ToastManager.toastMessages.collectLatest { toastMessage ->
            currentMessage = toastMessage
            visible = true
            val delayTime = when (toastMessage.duration) {
                ToastDuration.SHORT -> 2500L
                ToastDuration.LONG -> 3500L
            }
            delay(delayTime)
            visible = false
            delay(300) // 等待动画完成
            currentMessage = null
        }
    }
    
    // 根据主题设置背景和文字颜色
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White
    }
    val textColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Black.copy(alpha = 0.87f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible && currentMessage != null,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                animationSpec = tween(200),
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { it / 2 }
            )
        ) {
            currentMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = message.message,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
