package com.suseoaa.projectoaa.domain.course

import androidx.compose.runtime.Immutable

/**
 * 一天的作息时间表。
 *
 * 课表页、上课提醒、桌面小组件都要按同一份作息排版，所以定义放在这里由三方共用；
 * 以前小组件自己又抄了一份，学校调整作息时容易只改一处。
 */

enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

/**
 * 时间段配置。[sectionName] 对课程节次而言是"第几节"，对休息时段则是"午餐""午休"这类文字。
 */
@Immutable
data class TimeSlotConfig(
    val sectionName: String,
    val startTime: String,
    val endTime: String,
    val type: SlotType,
    val weight: Float
)

/**
 * 2025 年起启用的作息（11 节课）
 * 1-4节、5-8节、9-11节各为一个连续块，中间只有午休间隔
 */
val DailySchedulePost2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.0f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.0f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.0f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.0f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.0f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.0f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.0f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.0f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.0f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.0f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.0f)
)

/**
 * 2025 年之前的作息（12 节课）
 * 1-4节、5-8节、9-12节各为一个连续块，中间只有午休间隔
 */
val DailySchedulePre2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.0f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.0f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.0f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.0f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.0f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.0f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.0f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.0f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.0f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.0f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.0f),
    TimeSlotConfig("12", "21:30", "22:15", SlotType.CLASS, 1.0f)
)

/**
 * 按学年码挑作息表：2025 级起改成了 11 节课。
 *
 * @param xnm 学年码，如 "2026"
 */
fun dailyScheduleFor(xnm: String): List<TimeSlotConfig> =
    if (xnm >= "2025") DailySchedulePost2025 else DailySchedulePre2025
