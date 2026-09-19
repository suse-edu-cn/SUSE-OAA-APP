package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.shared.di.getSharedModules
import com.suseoaa.projectoaa.shared.util.AppLog
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalNativeApi::class)
fun initializeKoinIfNeeded() {
    // 与 Android 侧对齐：日志先于 Koin 初始化，Release 二进制不输出。
    AppLog.init(debug = Platform.isDebugBinary)
    try {
        KoinPlatform.getKoin()
    } catch (_: Throwable) {
        try {
            startKoin {
                modules(
                    getSharedModules() + listOf(platformModule(), appModule)
                )
            }
        } catch (_: Throwable) {
            // Already started concurrently
        }
    }
}

