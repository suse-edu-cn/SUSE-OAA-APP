package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.domain.checkin.SchedulerConfig
import com.suseoaa.projectoaa.shared.util.AppLog

/**
 * iOS 的后台任务由 Swift 侧 AppDelegate 的 BGTask 注册与触发，
 * Kotlin 侧只落配置，这里是空实现。
 */
class IosCheckinScheduler : PlatformCheckinScheduler {
    override fun schedule(config: SchedulerConfig) {
        AppLog.d("[iOS CheckinScheduler] schedule called (handled by Swift AppDelegate)")
    }

    override fun cancel() {
        AppLog.d("[iOS CheckinScheduler] cancel called (handled by Swift AppDelegate)")
    }
}
