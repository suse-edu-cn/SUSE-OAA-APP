package com.suseoaa.projectoaa.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * 全局「添加教务系统账号」弹窗请求。
 */
data class SchoolAccountPromptRequest(
    val featureName: String? = null,
    val onAccountAdded: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * 全局教务系统账号添加弹窗协调器。
 *
 * 各业务 ViewModel / Screen 在发现当前无可用教务账号时调用 [requestAddAccount]，
 * 由根部宿主组件统一弹出登录框并完成添加与默认账号设置。
 */
object SchoolAccountPromptManager {
    private val _activeRequest = MutableStateFlow<SchoolAccountPromptRequest?>(null)
    val activeRequest: StateFlow<SchoolAccountPromptRequest?> = _activeRequest.asStateFlow()

    fun requestAddAccount(
        featureName: String? = null,
        onAccountAdded: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val request = SchoolAccountPromptRequest(
            featureName = featureName,
            onAccountAdded = onAccountAdded,
            onDismiss = onDismiss
        )
        _activeRequest.compareAndSet(null, request)
    }

    fun dismiss() {
        val current = _activeRequest.getAndUpdate { null }
        current?.onDismiss?.invoke()
    }

    fun complete() {
        val current = _activeRequest.getAndUpdate { null }
        current?.onAccountAdded?.invoke()
    }
}
