package com.suseoaa.projectoaa.common.util

import android.Manifest
import android.content.Context
import android.os.Build

/**
 * 帮助类，用于封装权限请求逻辑。
 * 它通过启动一个透明的 PermissionRequestActivity 来处理权限，
 * 从而将调用方（如 WallpaperManager）与 Activity 的回调解耦。
 */
object PermissionHandler {

    /**
     * 在需要时请求存储权限。
     * 这将启动一个新的（透明的）Activity 来处理权限请求。
     */
    fun requestStoragePermission(context: Context) {
        // Android 10 (Q) 及以上版本使用 MediaStore 保存到公共目录不需要特定权限。
        // 如果在这些版本上仍然失败并触发了 SecurityException，
        // 这可能是 Scoped Storage 的其他限制或清单文件问题。
        // 最好的办法是引导用户到“设置”。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = PermissionRequestActivity.createIntent(
                context,
                permission = null,
                isSettingsFallback = true
            )
            context.startActivity(intent)
        }
        // Android 9 (P) 及以下版本，需要明确请求 WRITE_EXTERNAL_STORAGE
        else {
            val intent = PermissionRequestActivity.createIntent(
                context,
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                isSettingsFallback = false
            )
            context.startActivity(intent)
        }
    }
}