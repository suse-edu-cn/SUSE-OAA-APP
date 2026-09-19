package com.suseoaa.projectoaa.ui.screen.exam

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.datetime.LocalDate

// 考试时间字符串的解析。
//
// 教务返回的时间有两种写法："2025-01-15(09:00-11:00)" 和 "2025-01-15 09:00-11:00"，
// 编辑对话框要把它拆回年月日与起止时分。这是纯函数，拆出来后由
// ExamTimeParsingTest 覆盖两种格式与残缺输入。

/**
 * 解析考试日期
 * 支持格式: "2025-01-15(09:00-11:00)" 或 "2025-01-15 09:00-11:00"
 */
internal fun parseExamDate(timeStr: String): LocalDate? {
    return try {
        // 先尝试括号格式，再尝试空格格式
        val datePart = timeStr.substringBefore("(").takeIf { it != timeStr }
            ?: timeStr.split(" ").firstOrNull()
            ?: return null
        val parts = datePart.trim().split("-")
        if (parts.size >= 3) {
            LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * 提取时间部分（括号内或空格后的内容）
 */
internal fun extractTimePart(timeStr: String): String? {
    // 尝试括号格式: "2025-01-15(09:00-11:00)"
    val bracketContent = timeStr.substringAfter("(", "").substringBefore(")", "")
    if (bracketContent.contains("-") && bracketContent.contains(":")) {
        return bracketContent
    }
    // 尝试空格格式: "2025-01-15 09:00-11:00"
    return timeStr.split(" ").getOrNull(1)
}

/**
 * 解析开始小时
 */
internal fun parseExamStartHour(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val startTime = timePart.split("-").firstOrNull() ?: return null
        startTime.split(":").firstOrNull()?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析开始分钟
 */
internal fun parseExamStartMinute(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val startTime = timePart.split("-").firstOrNull() ?: return null
        startTime.split(":").getOrNull(1)?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析结束小时
 */
internal fun parseExamEndHour(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val endTime = timePart.split("-").getOrNull(1) ?: return null
        endTime.split(":").firstOrNull()?.toInt()
    } catch (e: Exception) {
        null
    }
}

/**
 * 解析结束分钟
 */
internal fun parseExamEndMinute(timeStr: String): Int? {
    return try {
        val timePart = extractTimePart(timeStr) ?: return null
        val endTime = timePart.split("-").getOrNull(1) ?: return null
        endTime.split(":").getOrNull(1)?.toInt()
    } catch (e: Exception) {
        null
    }
}
