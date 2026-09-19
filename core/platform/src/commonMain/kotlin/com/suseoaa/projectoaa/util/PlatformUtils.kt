package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 显示Toast消息 - 调用此函数触发Toast显示
 */
@Composable
fun showToast(message: String) {
    LaunchedEffect(message) {
        ToastManager.showToast(message)
    }
}

/**
 * 选择图片并返回字节数组
 */
@Composable
expect fun pickImageForAvatar(onImagePicked: (ByteArray?) -> Unit)
