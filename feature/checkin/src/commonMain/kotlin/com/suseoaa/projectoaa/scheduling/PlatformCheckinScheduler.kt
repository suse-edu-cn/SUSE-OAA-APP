package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.domain.checkin.SchedulerConfig

/**
 * 定时签到的平台调度能力。
 *
 * Android 走系统闹钟（AlarmManager），iOS 由 Swift 侧的 BGTask 处理，两者的实现
 * 都需要应用级的组件（AlarmManager 依赖 Context 与 BroadcastReceiver），因此实现
 * 留在应用模块，本模块只声明契约，由 DI 注入——这样 feature 不用反向依赖应用模块。
 */
interface PlatformCheckinScheduler {
    fun schedule(config: SchedulerConfig)
    fun cancel()
}
