package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.presentation.checkin.SchedulerConfig

actual class PlatformCheckinScheduler {
    actual fun schedule(config: SchedulerConfig) {
        // iOS 的 BGTask 注册在 Swift AppDelegate 中处理
        // Kotlin 侧只需保存配置，Swift 侧在 scheduleCheckinRefresh() 中读取
        println("[iOS PlatformCheckinScheduler] schedule called (handled by Swift AppDelegate)")
    }

    actual fun cancel() {
        println("[iOS PlatformCheckinScheduler] cancel called (handled by Swift AppDelegate)")
    }
}
