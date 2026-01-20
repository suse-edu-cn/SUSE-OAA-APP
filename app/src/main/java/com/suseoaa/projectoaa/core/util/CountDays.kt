package com.suseoaa.projectoaa.core.util

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
        val date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // 3. 分割开始和结束时间 -> ["09:30", "11:30"]
        val timeParts = timeRangePart.split("-")
        if (timeParts.size < 2) return null

        val startTime = LocalTime.parse(timeParts[0], DateTimeFormatter.ofPattern("HH:mm"))
        val endTime = LocalTime.parse(timeParts[1], DateTimeFormatter.ofPattern("HH:mm"))

        // 4. 组合成 LocalDateTime
        return LocalDateTime.of(date, startTime) to LocalDateTime.of(date, endTime)
    } catch (e: Exception) {
        // 解析失败（如格式不对或“时间待定”）返回 null
        return null
    }
}

fun getExamCountDown(timeStr: String): Pair<String, Color> {
    try {
        // 1. 尝试解析完整时间
        val timeRange = parseExamTimeRange(timeStr)
        if (timeRange == null) {
            // 解析失败（可能是"时间待定"），尝试只解析日期做兜底
            val datePart = timeStr.substringBefore("(")
            if (datePart.isNotBlank()) {
                val examDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val today = LocalDate.now()
                val daysDiff = ChronoUnit.DAYS.between(today, examDate)
                return when {
                    daysDiff < 0 -> "已结束" to Color.Gray
                    daysDiff == 0L -> "今天" to Color(0xFFFF3B30)
                    daysDiff == 1L -> "明天" to Color(0xFFFF9500)
                    else -> "${daysDiff}天" to Color(0xFF34C759)
                }
            }
            return "" to Color.Gray
        }

        val (startDateTime, endDateTime) = timeRange
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        // 2. 精确时间判断逻辑
        return when {
            // 当前时间晚于结束时间 -> 已结束
            now.isAfter(endDateTime) -> "已结束" to Color.Gray

            // 当前时间在开始和结束之间 -> 进行中
            now.isAfter(startDateTime) -> "进行中" to Color(0xFF34C759) // 绿色表示进行中

            // 还没开始 -> 计算倒计时
            else -> {
                val daysDiff = ChronoUnit.DAYS.between(today, startDateTime.toLocalDate())
                when {
                    daysDiff == 0L -> {
                        // 如果是今天，计算小时和分钟
                        val minutesTotal = ChronoUnit.MINUTES.between(now, startDateTime)
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