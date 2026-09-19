package com.suseoaa.projectoaa.scheduling

import android.content.Context
import com.suseoaa.projectoaa.domain.checkin.SchedulerConfig

/** 用系统闹钟实现定时签到；AlarmManager 需要 Context，故实现留在应用模块。 */
class AndroidCheckinScheduler(private val context: Context) : PlatformCheckinScheduler {
    override fun schedule(config: SchedulerConfig) {
        CheckinAlarmManager.scheduleNextAlarm(context, config)
    }

    override fun cancel() {
        CheckinAlarmManager.cancelAlarm(context)
    }
}
