package com.suseoaa.projectoaa.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.suseoaa.projectoaa.domain.checkin.CheckinTimeCalculator
import com.suseoaa.projectoaa.domain.checkin.SchedulerConfig
import com.suseoaa.projectoaa.shared.util.AppLog

object CheckinAlarmManager {

    private const val ACTION_CHECKIN_ALARM = "com.suseoaa.projectoaa.CHECKIN_ALARM"
    private const val REQUEST_CODE = 2001

    fun scheduleNextAlarm(context: Context, config: SchedulerConfig) {
        if (!config.enabled || config.targetAccountIds.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)

        val triggerTimeMillis = CheckinTimeCalculator.calculateNextRunTimeEpochMillis(
            config.scheduledHour, config.scheduledMinute, config.scheduledSecond
        )

        AppLog.d("[CheckinAlarmManager] 准备设置闹钟: ${CheckinTimeCalculator.formatTime(config.scheduledHour, config.scheduledMinute, config.scheduledSecond)}, trigger=$triggerTimeMillis")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要检查精确闹钟权限
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                        pendingIntent
                    )
                    AppLog.d("[CheckinAlarmManager] 精确闹钟已设置 (setAlarmClock)")
                } else {
                    // 没有精确闹钟权限，使用 setAndAllowWhileIdle 作为备用（精度较低但可用）
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    AppLog.d("[CheckinAlarmManager] 无精确闹钟权限，使用 setAndAllowWhileIdle 备用方案")
                }
            } else {
                // Android 12 以下直接使用精确闹钟
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
                AppLog.d("[CheckinAlarmManager] 精确闹钟已设置 (legacy)")
            }
        } catch (e: SecurityException) {
            AppLog.e("[CheckinAlarmManager] 设置精确闹钟失败(SecurityException): ${e.message}，尝试备用方案")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                AppLog.d("[CheckinAlarmManager] 备用闹钟已设置 (setAndAllowWhileIdle)")
            } catch (e2: Exception) {
                AppLog.e("[CheckinAlarmManager] 备用闹钟也设置失败: ${e2.message}")
            }
        } catch (e: Exception) {
            AppLog.e("[CheckinAlarmManager] 设置闹钟失败: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        AppLog.d("[CheckinAlarmManager] 闹钟已取消")
    }

    fun isAlarmScheduled(context: Context): Boolean {
        val intent = Intent(ACTION_CHECKIN_ALARM).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
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
