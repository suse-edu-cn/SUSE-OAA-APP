package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * iOS平台的权限管理器实现
 */
actual class PlatformPermissionManager actual constructor() {
    actual fun hasNearFieldPermissions(): Boolean {
        // iOS上局域网发现通常在首次调用时由系统弹出“本地网络”权限弹窗
        // 开发者需在Info.plist中配置NSLocalNetworkUsageDescription和NSBonjourServices
        return true
    }

    @Composable
    actual fun RequestNearFieldPermissions(onResult: (Boolean) -> Unit) {
        LaunchedEffect(Unit) {
            // iOS系统会自动管理权限弹窗，这里直接返回成功以允许逻辑继续
            onResult(true)
        }
    }
    
    actual fun isHardwareEnabled(): Boolean {
        // iOS无法直接静默检查或开启Wi-Fi开关，通常依赖系统自动弹窗或网络状态监听
        return true
    }

    @Composable
    actual fun RequestEnableHardware(onResult: (Boolean) -> Unit) {
        LaunchedEffect(Unit) {
            onResult(true)
        }
    }
}
