package com.suseoaa.projectoaa.util

import androidx.compose.ui.graphics.Color
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import kotlinx.datetime.*

/**
 * 获取考试倒计时文本和对应颜色（UI 层工具）
 */
fun getExamCountDown(timeStr: String): Pair<String, Color> {
    try {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val today = now.date

        // 1. 尝试解析完整时间
        val timeRange = parseExamTimeRange(timeStr)
        if (timeRange == null) {
            // 解析失败（可能是"时间待定"），尝试只解析日期做兜底
            val datePart = timeStr.substringBefore("(")
            if (datePart.isNotBlank()) {
                try {
                    val examDate = LocalDate.parse(datePart)
                    val daysDiff = today.daysUntil(examDate).toLong()
                    return when {
                        daysDiff < 0 -> "已结束" to Color.Gray
                        daysDiff == 0L -> "今天" to Color(0xFFFF3B30)
                        daysDiff == 1L -> "明天" to Color(0xFFFF9500)
                        else -> "${daysDiff}天" to Color(0xFF34C759)
                    }
                } catch (e: Exception) {
                    return "" to Color.Gray
                }
            }
            return "" to Color.Gray
        }

        val (startDateTime, endDateTime) = timeRange

        // 2. 精确时间判断逻辑
        return when {
            // 当前时间晚于结束时间 -> 已结束
            now > endDateTime -> "已结束" to Color.Gray

            // 当前时间在开始和结束之间 -> 进行中
            now >= startDateTime && now <= endDateTime -> "进行中" to Color(0xFF34C759)

            // 还没开始 -> 计算倒计时
            else -> {
                val daysDiff = today.daysUntil(startDateTime.date).toLong()
                when {
                    daysDiff == 0L -> {
                        val nowInstant = now.toInstant(timeZone)
                        val startInstant = startDateTime.toInstant(timeZone)
                        val minutesTotal =
                            (startInstant.toEpochMilliseconds() - nowInstant.toEpochMilliseconds()) / (1000 * 60)
                        val hours = minutesTotal / 60
                        val minutes = minutesTotal % 60
                        if (hours > 0) {
                            "${hours}小时${minutes}分" to Color(0xFFFF3B30)
                        } else {
                            "${minutes}分钟" to Color(0xFFFF3B30)
                        }
                    }

                    daysDiff == 1L -> "明天" to Color(0xFFFF9500)
                    daysDiff <= 7L -> "${daysDiff}天后" to Color(0xFF007AFF)
                    else -> "${daysDiff}天" to Color(0xFF34C759)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "" to Color.Gray
    }
}
