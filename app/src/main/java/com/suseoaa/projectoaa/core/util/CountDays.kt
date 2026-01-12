package com.suseoaa.projectoaa.core.util

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun getExamCountDown(timeStr: String): Pair<String, Color> {
    // timeStr 示例: "2026-01-08(09:30-11:30)"
    try {
        // 1. 提取日期部分 "2026-01-08"
        // 考试信息把日期和时间存在了一起了，所以需要截取括号前面的部分
        val datePart = timeStr.substringBefore("(")
        if (datePart.isBlank()) return "" to Color.Gray

        // 2. 解析日期
        val examDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val today = LocalDate.now()

        // 3. 计算天数差
        val daysDiff = ChronoUnit.DAYS.between(today, examDate)

        // 4. 根据天数返回不同的提示和颜色
        return when {
            daysDiff < 0 -> "已结束" to Color.Gray
            daysDiff == 0L -> "今天" to Color(0xFFFF3B30) // 红色警告
            daysDiff == 1L -> "明天" to Color(0xFFFF9500) // 橙色紧急
            daysDiff <= 7L -> "${daysDiff}天后" to Color(0xFF007AFF) // 蓝色提醒
            else -> "${daysDiff}天" to Color(0xFF34C759) // 绿色宽裕
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "" to Color.Gray
    }
}