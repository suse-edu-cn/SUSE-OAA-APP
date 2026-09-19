package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 跨平台的返回键处理器
 * 在 Android 上使用 BackHandler，在 iOS 上为空实现（iOS 没有系统返回键）
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
