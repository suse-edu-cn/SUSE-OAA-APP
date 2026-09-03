package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 签到模块统一的时间工具。
 *
 * 签到相关接口与数据库都使用 "yyyy-MM-dd HH:mm:ss" 这一种字符串格式，
 * 之前这段格式化代码在两个仓库里被复制了近十份，统一收敛到这里。
 */
object CheckinClock {

    /** 当前本地时间 */
    fun now(): LocalDateTime =
        OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())

    /** 当前时间字符串，格式 "yyyy-MM-dd HH:mm:ss" */
    fun nowString(): String = format(now())

    /** N 天之后的时间字符串 */
    fun afterDays(days: Int): String = afterMillis(days * 24L * 60L * 60L * 1000L)

    /** N 小时之后的时间字符串 */
    fun afterHours(hours: Int): String = afterMillis(hours * 60L * 60L * 1000L)

    /** 把本地时间格式化成 "yyyy-MM-dd HH:mm:ss" */
    fun format(dateTime: LocalDateTime): String {
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        val second = dateTime.second.toString().padStart(2, '0')
        return "${dateTime.date} $hour:$minute:$second"
    }

    private fun afterMillis(offsetMillis: Long): String {
        val target = Instant.fromEpochMilliseconds(
            OaaClock.now().toEpochMilliseconds() + offsetMillis
        )
        return format(target.toLocalDateTime(TimeZone.currentSystemDefault()))
    }
}
