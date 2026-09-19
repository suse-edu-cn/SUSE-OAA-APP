package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 跨平台权限管理器接口
 * 负责处理各平台特定的权限检查与请求逻辑
 */
expect class PlatformPermissionManager() {
    /**
     * 检查是否已授予近场搜索所需的权限
     * 在Android上涉及位置或附近设备权限，在iOS上通常直接返回true（由系统在调用API时触发弹窗）
     */
    fun hasNearFieldPermissions(): Boolean

    /**
     * 请求近场搜索所需的权限
     * @param onResult 权限请求结果的回调
     */
    @Composable
    fun RequestNearFieldPermissions(onResult: (Boolean) -> Unit)

    /**
     * 检查硬件开关（Wi-Fi/蓝牙）是否已开启
     */
    fun isHardwareEnabled(): Boolean

    /**
     * 引导用户开启硬件开关
     * @param onResult 处理后的回调
     */
    @Composable
    fun RequestEnableHardware(onResult: (Boolean) -> Unit)
}
