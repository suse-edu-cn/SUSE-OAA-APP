package com.suseoaa.projectoaa.util

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android 实现：使用 BackHandler
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
