package com.suseoaa.projectoaa.presentation.update

import platform.Foundation.NSBundle

/**
 * iOS 平台标识
 */
actual fun isIosPlatform(): Boolean = true

/**
 * 获取应用版本号
 */
actual fun getAppVersionName(): String {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
        ?: "1.0.0"
}
