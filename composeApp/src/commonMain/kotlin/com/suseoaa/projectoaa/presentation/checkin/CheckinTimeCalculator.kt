package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object CheckinTimeCalculator {

    private val tz = TimeZone.of("Asia/Shanghai")

    fun calculateNextRunTimeEpochMillis(hour: Int, minute: Int, second: Int = 0): Long {
        return calculateNextRunTime(hour, minute, second).toEpochMilliseconds()
    }

    fun calculateNextRunTime(hour: Int, minute: Int, second: Int = 0): Instant {
        val now = OaaClock.now().toLocalDateTime(tz)

        var target = LocalDateTime(now.date.year, now.date.monthNumber, now.date.dayOfMonth, hour, minute, second)

        if (target.toInstant(tz) <= now.toInstant(tz)) {
            val tomorrow = now.date.plus(1, DateTimeUnit.DAY)
            target = LocalDateTime(tomorrow.year, tomorrow.monthNumber, tomorrow.dayOfMonth, hour, minute, second)
        }

        return target.toInstant(tz)
    }

    fun formatTime(hour: Int, minute: Int, second: Int = 0): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}"
    }

    fun formatCurrentTime(): String {
        val now = OaaClock.now().toLocalDateTime(tz)
        return "${now.date} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }
}
