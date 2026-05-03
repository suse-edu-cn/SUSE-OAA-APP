package com.suseoaa.projectoaa.scheduling

import android.content.Context
import com.suseoaa.projectoaa.presentation.checkin.SchedulerConfig

actual class PlatformCheckinScheduler(private val context: Context) {
    actual fun schedule(config: SchedulerConfig) {
        CheckinAlarmManager.scheduleNextAlarm(context, config)
    }

    actual fun cancel() {
        CheckinAlarmManager.cancelAlarm(context)
    }
}
