package com.suseoaa.projectoaa.presentation.update

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android 平台标识
 */
actual fun isIosPlatform(): Boolean = false

/**
 * 获取应用版本号
 */
actual fun getAppVersionName(): String {
    return VersionHelper.getVersionName()
}

private object VersionHelper : KoinComponent {
    private val context: Context by inject()
    
    fun getVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
