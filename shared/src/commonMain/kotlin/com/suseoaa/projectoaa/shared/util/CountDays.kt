package com.suseoaa.projectoaa.shared.util

import kotlinx.datetime.*

/**
 * 考试倒计时工具类
 * 替代原生 Android 的 java.time 实现，使用 kotlinx-datetime
 */

/**
 * 解析考试时间字符串，返回开始时间和结束时间
 * 支持格式: "2026-01-08(09:30-11:30)" 或 "2026-01-08 09:30-11:30"
 */
fun parseExamTimeRange(timeStr: String): Pair<LocalDateTime, LocalDateTime>? {
    try {
        val datePart: String
        val timeRangePart: String

        if (timeStr.contains("(")) {
            // 括号格式: "2026-01-08(09:30-11:30)"
            val parts = timeStr.split("(")
            if (parts.size < 2) return null
            datePart = parts[0]
            timeRangePart = parts[1].removeSuffix(")")
        } else if (timeStr.contains(" ")) {
            // 空格格式: "2026-01-08 09:30-11:30"
            val parts = timeStr.split(" ", limit = 2)
            if (parts.size < 2) return null
            datePart = parts[0]
            timeRangePart = parts[1]
        } else {
            return null
        }

        // 解析日期
        val date = LocalDate.parse(datePart)

        // 分割开始和结束时间 -> ["09:30", "11:30"]
        val timeParts = timeRangePart.split("-")
        if (timeParts.size < 2) return null

        val startTime = LocalTime.parse(timeParts[0])
        val endTime = LocalTime.parse(timeParts[1])

        // 组合成 LocalDateTime
        return LocalDateTime(date, startTime) to LocalDateTime(date, endTime)
    } catch (e: Exception) {
        // 解析失败（如格式不对或"时间待定"）返回 null
        return null
    }
}

/**
 * 获取当前学期 (xnm, xqm)
 */
fun getCurrentTerm(): Pair<String, String> {
    val now = com.suseoaa.projectoaa.shared.util.OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val year = now.year
    val month = now.monthNumber
    return if (month >= 8 || month == 1) {
        val xnm = if (month == 1) (year - 1).toString() else year.toString()
        xnm to "3"
    } else {
        (year - 1).toString() to "12"
    }
}
