package com.suseoaa.projectoaa.composeapp.widget

import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.DateTimeUnit
import org.koin.core.context.GlobalContext

object WidgetDataFetcher {

    enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

    data class TimeSlotConfig(
        val sectionName: String,
        val startTime: String,
        val endTime: String,
        val type: SlotType,
        val weight: Float
    )

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

    fun getDailySchedule(): List<TimeSlotConfig> {
        val (year, _) = calculateCurrentRealTerm()
        return if (year >= "2025") DailySchedulePost2025 else DailySchedulePre2025
    }

    suspend fun getActiveCourses(): List<CourseWithTimes> {
        return try {
            val koin = GlobalContext.get()
            val tokenManager = koin.get<TokenManager>()
            val courseRepo = koin.get<LocalCourseRepository>()

            // Get current student ID
            val studentId = tokenManager.currentStudentId.first() ?: return emptyList()

            val (xnm, xqm) = calculateCurrentRealTerm()
            val allCourses = courseRepo.getCourses(studentId, xnm, xqm).first()

            allCourses
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCurrentWeek(): Int {
        return try {
            val koin = GlobalContext.get()
            val tokenManager = koin.get<TokenManager>()
            
            val savedDateStr = tokenManager.getSemesterStartDate()
            val hasWeekZero = tokenManager.getSemesterHasWeekZero()
            val minWeek = if (hasWeekZero) 0 else 1
            
            val startDate = if (savedDateStr != null) {
                try {
                    LocalDate.parse(savedDateStr)
                } catch (e: Exception) {
                    getCurrentMonday()
                }
            } else {
                getCurrentMonday()
            }
            
            val todayDate = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val daysBetween = startDate.daysUntil(todayDate)
            val weekNum = (daysBetween / 7) + minWeek
            
            weekNum.coerceIn(minWeek, 25)
        } catch (e: Exception) {
            1
        }
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val month = now.monthNumber
        val year = now.year

        return if (month >= 9) {
            // 9月及以后：当年第一学期
            year.toString() to "3"
        } else if (month >= 2) {
            // 2-8月：上一年第二学期
            (year - 1).toString() to "12"
        } else {
            // 1月：上一年第一学期
            (year - 1).toString() to "3"
        }
    }

    private fun getCurrentMonday(): LocalDate {
        val today = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }

    fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean {
        if (weeksStr.isNotBlank()) {
            return parseWeeksString(weeksStr, week)
        }
        if (mask != 0L) {
            return (mask and (1L shl (week - 1))) != 0L
        }
        return true
    }

    private fun parseWeeksString(weeksStr: String, targetWeek: Int): Boolean {
        if (weeksStr.isBlank()) return true
        val cleanStr = weeksStr
            .replace("周", "")
            .replace("(单)", "#ODD#")
            .replace("（单）", "#ODD#")
            .replace("(双)", "#EVEN#")
            .replace("（双）", "#EVEN#")
            .replace("，", ",")
            .replace("；", ",")
            .replace(";", ",")
            .replace("单", "")
            .replace("双", "")
            .replace(" ", "")

        val parts = cleanStr.split(",")
        val hasSegmentParityTag = parts.any { it.contains("#ODD#") || it.contains("#EVEN#") }
        val globalOddOnly = !hasSegmentParityTag && weeksStr.contains("单") && !weeksStr.contains("双")
        val globalEvenOnly = !hasSegmentParityTag && weeksStr.contains("双") && !weeksStr.contains("单")

        for (part in parts) {
            val isOddOnly = part.contains("#ODD#") || (globalOddOnly && !part.contains("#EVEN#"))
            val isEvenOnly = part.contains("#EVEN#") || (globalEvenOnly && !part.contains("#ODD#"))
            val cleanPart = part.replace("#ODD#", "").replace("#EVEN#", "")

            if (cleanPart.contains("-")) {
                val range = cleanPart.split("-")
                if (range.size == 2) {
                    val start = range[0].toIntOrNull() ?: continue
                    val end = range[1].toIntOrNull() ?: continue
                    if (targetWeek in start..end) {
                        val weekMatches = when {
                            isOddOnly -> targetWeek % 2 == 1
                            isEvenOnly -> targetWeek % 2 == 0
                            else -> true
                        }
                        if (weekMatches) return true
                    }
                }
            } else {
                val single = cleanPart.toIntOrNull()
                if (single == targetWeek) {
                    val weekMatches = when {
                        isOddOnly -> targetWeek % 2 == 1
                        isEvenOnly -> targetWeek % 2 == 0
                        else -> true
                    }
                    if (weekMatches) return true
                }
            }
        }
        return false
    }

    fun parseWeekday(weekday: String): Int {
        return when {
            weekday.contains("一") || weekday == "1" -> 1
            weekday.contains("二") || weekday == "2" -> 2
            weekday.contains("三") || weekday == "3" -> 3
            weekday.contains("四") || weekday == "4" -> 4
            weekday.contains("五") || weekday == "5" -> 5
            weekday.contains("六") || weekday == "6" -> 6
            weekday.contains("日") || weekday.contains("天") || weekday == "7" -> 7
            else -> weekday.toIntOrNull() ?: 1
        }
    }

    fun parsePeriod(period: String): Pair<Int, Int> {
        val cleanPeriod = period.replace("节", "").trim()
        return if (cleanPeriod.contains("-")) {
            val parts = cleanPeriod.split("-")
            val start = parts[0].toIntOrNull() ?: 1
            val end = parts.getOrNull(1)?.toIntOrNull() ?: start
            start to (end - start + 1)
        } else {
            val single = cleanPeriod.toIntOrNull() ?: 1
            single to 1
        }
    }

    suspend fun getNextCourse(): Pair<CourseWithTimes, TimeSlotConfig>? {
        val courses = getActiveCourses()
        val currentWeek = getCurrentWeek()
        
        // Filter courses for current week
        val currentWeekCourses = courses.filter { courseWithTimes ->
            courseWithTimes.times.any { time ->
                isWeekActive(currentWeek, time.weeks, time.weeksMask)
            }
        }
        
        val schedule = getDailySchedule()
        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDay = now.dayOfWeek.value // 1..7 (Monday..Sunday)
        val currentHour = now.hour
        val currentMinute = now.minute
        val currentTotalMinutes = currentHour * 60 + currentMinute
        
        // Try to find a course today that hasn't started yet
        val todayCourses = currentWeekCourses.filter { courseWithTimes ->
            courseWithTimes.times.any { time ->
                isWeekActive(currentWeek, time.weeks, time.weeksMask) && parseWeekday(time.weekday) == currentDay
            }
        }
        
        var nextCourseToday: Pair<CourseWithTimes, TimeSlotConfig>? = null
        var minTimeDiff = Int.MAX_VALUE
        
        for (course in todayCourses) {
            for (time in course.times) {
                if (!isWeekActive(currentWeek, time.weeks, time.weeksMask) || parseWeekday(time.weekday) != currentDay) continue
                
                val (startPeriod, _) = parsePeriod(time.period)
                val slot = schedule.find { it.sectionName == startPeriod.toString() } ?: continue
                
                if (slot.startTime.isEmpty()) continue
                val parts = slot.startTime.split(":")
                if (parts.size != 2) continue
                
                val slotTotalMinutes = parts[0].toInt() * 60 + parts[1].toInt()
                if (slotTotalMinutes > currentTotalMinutes) {
                    val diff = slotTotalMinutes - currentTotalMinutes
                    if (diff < minTimeDiff) {
                        minTimeDiff = diff
                        nextCourseToday = course to slot
                    }
                }
            }
        }
        
        if (nextCourseToday != null) {
            return nextCourseToday
        }
        
        // Find first course tomorrow
        val tomorrowDay = if (currentDay == 7) 1 else currentDay + 1
        val tomorrowWeek = if (currentDay == 7) currentWeek + 1 else currentWeek
        
        val tomorrowCourses = courses.filter { courseWithTimes ->
            courseWithTimes.times.any { time ->
                isWeekActive(tomorrowWeek, time.weeks, time.weeksMask) && parseWeekday(time.weekday) == tomorrowDay
            }
        }
        
        var firstCourseTomorrow: Pair<CourseWithTimes, TimeSlotConfig>? = null
        var earliestTime = Int.MAX_VALUE
        
        for (course in tomorrowCourses) {
            for (time in course.times) {
                if (!isWeekActive(tomorrowWeek, time.weeks, time.weeksMask) || parseWeekday(time.weekday) != tomorrowDay) continue
                
                val (startPeriod, _) = parsePeriod(time.period)
                val slot = schedule.find { it.sectionName == startPeriod.toString() } ?: continue
                
                if (slot.startTime.isEmpty()) continue
                val parts = slot.startTime.split(":")
                if (parts.size != 2) continue
                
                val slotTotalMinutes = parts[0].toInt() * 60 + parts[1].toInt()
                if (slotTotalMinutes < earliestTime) {
                    earliestTime = slotTotalMinutes
                    firstCourseTomorrow = course to slot
                }
            }
        }
        
        return firstCourseTomorrow
    }

    data class TodaySchedule(
        val morning: List<Pair<CourseWithTimes, TimeSlotConfig>>,
        val afternoon: List<Pair<CourseWithTimes, TimeSlotConfig>>,
        val evening: List<Pair<CourseWithTimes, TimeSlotConfig>>
    )

    suspend fun getTodayCourses(): TodaySchedule {
        val courses = getActiveCourses()
        val currentWeek = getCurrentWeek()
        
        val schedule = getDailySchedule()
        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDay = now.dayOfWeek.value // 1..7
        
        val todayCoursesList = mutableListOf<Pair<CourseWithTimes, TimeSlotConfig>>()
        
        for (course in courses) {
            for (time in course.times) {
                if (!isWeekActive(currentWeek, time.weeks, time.weeksMask) || parseWeekday(time.weekday) != currentDay) continue
                
                val (startPeriod, _) = parsePeriod(time.period)
                val slot = schedule.find { it.sectionName == startPeriod.toString() } ?: continue
                if (slot.startTime.isEmpty()) continue
                
                todayCoursesList.add(course to slot)
            }
        }
        
        // Sort by start time
        todayCoursesList.sortBy {
            val parts = it.second.startTime.split(":")
            if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
        }
        
        val morning = mutableListOf<Pair<CourseWithTimes, TimeSlotConfig>>()
        val afternoon = mutableListOf<Pair<CourseWithTimes, TimeSlotConfig>>()
        val evening = mutableListOf<Pair<CourseWithTimes, TimeSlotConfig>>()
        
        for (item in todayCoursesList) {
            val parts = item.second.startTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                if (hour < 12) {
                    morning.add(item)
                } else if (hour < 18) {
                    afternoon.add(item)
                } else {
                    evening.add(item)
                }
            }
        }
        
        return TodaySchedule(morning, afternoon, evening)
    }

    suspend fun getWeeklyCourses(): Map<Int, List<Pair<CourseWithTimes, TimeSlotConfig>>> {
        val courses = getActiveCourses()
        val currentWeek = getCurrentWeek()
        
        val schedule = getDailySchedule()
        
        val weeklyMap = mutableMapOf<Int, MutableList<Pair<CourseWithTimes, TimeSlotConfig>>>()
        for (i in 1..7) {
            weeklyMap[i] = mutableListOf()
        }
        
        for (course in courses) {
            for (time in course.times) {
                if (!isWeekActive(currentWeek, time.weeks, time.weeksMask)) continue
                
                val day = parseWeekday(time.weekday)
                if (day !in 1..7) continue
                
                val (startPeriod, _) = parsePeriod(time.period)
                val slot = schedule.find { it.sectionName == startPeriod.toString() } ?: continue
                if (slot.startTime.isEmpty()) continue
                
                weeklyMap[day]?.add(course to slot)
            }
        }
        
        // Sort courses in each day
        for (day in 1..7) {
            weeklyMap[day]?.sortBy {
                val parts = it.second.startTime.split(":")
                if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
            }
        }
        
        return weeklyMap
    }
}
