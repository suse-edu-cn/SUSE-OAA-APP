package com.suseoaa.projectoaa.util

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.*

/**
 * 考试倒计时工具类
 * 替代原生 Android 的 java.time 实现，使用 kotlinx-datetime
 */

/**
 * 解析考试时间字符串，返回开始时间和结束时间
 * 输入格式示例: "2026-01-08(09:30-11:30)"
 */
fun parseExamTimeRange(timeStr: String): Pair<LocalDateTime, LocalDateTime>? {
    try {
        // 1. 分割日期和时间段 -> ["2026-01-08", "09:30-11:30)"]
        val parts = timeStr.split("(")
        if (parts.size < 2) return null

        val datePart = parts[0]
        val timeRangePart = parts[1].removeSuffix(")") // 去掉右括号

        // 2. 解析日期
        val date = LocalDate.parse(datePart)

        // 3. 分割开始和结束时间 -> ["09:30", "11:30"]
        val timeParts = timeRangePart.split("-")
        if (timeParts.size < 2) return null

        val startTime = LocalTime.parse(timeParts[0])
        val endTime = LocalTime.parse(timeParts[1])

        // 4. 组合成 LocalDateTime
        return LocalDateTime(date, startTime) to LocalDateTime(date, endTime)
    } catch (e: Exception) {
        // 解析失败（如格式不对或"时间待定"）返回 null
        return null
    }
}

/**
 * 获取考试倒计时文本和对应颜色
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
            now >= startDateTime && now <= endDateTime -> "进行中" to Color(0xFF34C759) // 绿色表示进行中

            // 还没开始 -> 计算倒计时
            else -> {
                val daysDiff = today.daysUntil(startDateTime.date).toLong()
                when {
                    daysDiff == 0L -> {
                        // 如果是今天，计算小时和分钟
                        val nowInstant = now.toInstant(timeZone)
                        val startInstant = startDateTime.toInstant(timeZone)
                        val minutesTotal = (startInstant.toEpochMilliseconds() - nowInstant.toEpochMilliseconds()) / (1000 * 60)
                        val hours = minutesTotal / 60
                        val minutes = minutesTotal % 60
                        if (hours > 0) {
                            "${hours}小时${minutes}分" to Color(0xFFFF3B30) // 红色紧迫
                        } else {
                            "${minutes}分钟" to Color(0xFFFF3B30)
                        }
                    }

                    daysDiff == 1L -> "明天" to Color(0xFFFF9500) // 橙色
                    daysDiff <= 7L -> "${daysDiff}天后" to Color(0xFF007AFF) // 蓝色
                    else -> "${daysDiff}天" to Color(0xFF34C759) // 绿色
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "" to Color.Gray
    }
}

/**
 * 获取当前学期 (xnm, xqm)
 */
fun getCurrentTerm(): Pair<String, String> {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val year = now.year
    val month = now.monthNumber
    return if (month >= 8 || month == 1) {
        val xnm = if (month == 1) (year - 1).toString() else year.toString()
        xnm to "3"
    } else {
        (year - 1).toString() to "12"
    }
}
