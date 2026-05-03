package com.suseoaa.projectoaa.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.suseoaa.projectoaa.presentation.checkin.CheckinTimeCalculator
import com.suseoaa.projectoaa.presentation.checkin.SchedulerConfig

object CheckinAlarmManager {

    private const val ACTION_CHECKIN_ALARM = "com.suseoaa.projectoaa.CHECKIN_ALARM"
    private const val REQUEST_CODE = 2001

    fun scheduleNextAlarm(context: Context, config: SchedulerConfig) {
        if (!config.enabled || config.targetAccountIds.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)

        val triggerTimeMillis = CheckinTimeCalculator.calculateNextRunTimeEpochMillis(
            config.scheduledHour, config.scheduledMinute
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                pendingIntent
            )
            println("[CheckinAlarmManager] 闹钟已设置: ${CheckinTimeCalculator.formatTime(config.scheduledHour, config.scheduledMinute)}, 触发时间: $triggerTimeMillis")
        } catch (e: SecurityException) {
            println("[CheckinAlarmManager] 设置闹钟失败: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        println("[CheckinAlarmManager] 闹钟已取消")
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(ACTION_CHECKIN_ALARM).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
