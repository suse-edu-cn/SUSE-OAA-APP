package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 跨平台 Toast 显示组件
 * 需要在 App 根组件中放置，用于收集并显示 Toast 消息
 */
@Composable
expect fun ToastHandler()
