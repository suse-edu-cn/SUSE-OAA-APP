package com.suseoaa.projectoaa.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 跨平台 Toast 管理器
 * 使用 SharedFlow 来发送 Toast 消息，由平台特定实现来显示
 */
object ToastManager {
    private val _toastMessages = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 10)
    val toastMessages: SharedFlow<ToastMessage> = _toastMessages.asSharedFlow()

    fun showToast(message: String, duration: ToastDuration = ToastDuration.SHORT) {
        _toastMessages.tryEmit(ToastMessage(message, duration))
    }

    fun showSuccess(message: String) {
        showToast(message, ToastDuration.SHORT)
    }

    fun showError(message: String) {
        showToast(message, ToastDuration.LONG)
    }
}

data class ToastMessage(
    val message: String,
    val duration: ToastDuration = ToastDuration.SHORT
)

enum class ToastDuration {
    SHORT,
    LONG
}
