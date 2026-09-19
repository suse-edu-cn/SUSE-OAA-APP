package com.suseoaa.projectoaa.composeapp.widget

import com.suseoaa.projectoaa.domain.course.DailySchedulePost2025
import com.suseoaa.projectoaa.domain.course.TimeSlotConfig
import com.suseoaa.projectoaa.domain.course.dailyScheduleFor
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamCacheEntity
import com.suseoaa.projectoaa.shared.domain.course.CourseScheduleParser
import com.suseoaa.projectoaa.shared.domain.course.SemesterCalendar
import com.suseoaa.projectoaa.shared.domain.course.PeriodSpan
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.util.OaaClock
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.DateTimeUnit
import org.koin.core.context.GlobalContext
import com.suseoaa.projectoaa.shared.data.local.store.SemesterStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore

object WidgetDataFetcher {

    fun getDailySchedule(): List<TimeSlotConfig> {
        val (year, _) = calculateCurrentRealTerm()
        return dailyScheduleFor(year)
    }

    suspend fun getActiveCourses(): List<CourseWithTimes> {
        return try {
            val koin = GlobalContext.get()
            val semesterStore = koin.get<SemesterStore>()
            val sessionStore = koin.get<SessionStore>()
            val courseRepo = koin.get<LocalCourseRepository>()

            // 获取当前学生 ID
            val studentId = sessionStore.currentStudentId.first() ?: return emptyList()

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
            val semesterStore = koin.get<SemesterStore>()
            val sessionStore = koin.get<SessionStore>()
            
            val savedDateStr = semesterStore.getSemesterStartDate()
            val hasWeekZero = semesterStore.getSemesterHasWeekZero()

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
            SemesterCalendar.currentWeek(startDate, todayDate, hasWeekZero)
        } catch (e: Exception) {
            1
        }
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        return com.suseoaa.projectoaa.shared.util.getCurrentTerm()
    }

    private fun getCurrentMonday(): LocalDate {
        val today = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }

    fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean =
        CourseScheduleParser.isWeekActive(week, weeksStr, mask)

    fun parseWeekday(weekday: String): Int =
        CourseScheduleParser.parseWeekday(weekday)

    fun parsePeriod(period: String): PeriodSpan =
        CourseScheduleParser.parsePeriod(period)

    suspend fun getNextCourse(): Pair<CourseWithTimes, TimeSlotConfig>? {
        val courses = getActiveCourses()
        val currentWeek = getCurrentWeek()
        
        // 过滤本周课程
        val currentWeekCourses = courses.filter { courseWithTimes ->
            courseWithTimes.times.any { time ->
                isWeekActive(currentWeek, time.weeks, time.weeksMask)
            }
        }
        
        val schedule = getDailySchedule()
        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDay = now.dayOfWeek.ordinal + 1 // 1..7 (Monday..Sunday)
        val currentHour = now.hour
        val currentMinute = now.minute
        val currentTotalMinutes = currentHour * 60 + currentMinute
        
        // 尝试寻找今天尚未开始的课程
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
        
        // 寻找明天的第一节课
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
        val currentDay = now.dayOfWeek.ordinal + 1 // 1..7
        
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
        
        // 按开始时间排序
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
        
        // 对每天的课程进行排序
        for (day in 1..7) {
            weeklyMap[day]?.sortBy {
                val parts = it.second.startTime.split(":")
                if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
            }
        }
        
        return weeklyMap
    }

    data class ExamsSummary(
        val upcoming: List<ExamCacheEntity>,
        val takenCount: Int,
        val unTakenCount: Int
    )

    suspend fun getExamsSummary(): ExamsSummary {
        return try {
            val koin = GlobalContext.get()
            val semesterStore = koin.get<SemesterStore>()
            val sessionStore = koin.get<SessionStore>()
            val schoolInfoRepo = koin.get<SchoolInfoRepository>()

            val studentId = sessionStore.currentStudentId.first() ?: return ExamsSummary(emptyList(), 0, 0)
            
            // 从本地缓存获取所有考试
            val exams = schoolInfoRepo.observeExams(studentId).first()
            
            val timeZone = TimeZone.currentSystemDefault()
            val now = OaaClock.now().toLocalDateTime(timeZone)
            
            var taken = 0
            var untaken = 0
            val upcoming = mutableListOf<ExamCacheEntity>()

            exams.forEach { exam ->
                val timeRange = parseExamTimeRange(exam.time)
                if (timeRange != null) {
                    if (now > timeRange.second) {
                        taken++
                    } else {
                        untaken++
                        upcoming.add(exam)
                    }
                } else {
                    // 如果时间未知，假设未考但不显示在近期列表中
                    untaken++
                }
            }

            val sortedUpcoming = upcoming.sortedBy { exam ->
                parseExamTimeRange(exam.time)?.first
            }.take(3)

            ExamsSummary(sortedUpcoming, taken, untaken)
        } catch (e: Exception) {
            e.printStackTrace()
            ExamsSummary(emptyList(), 0, 0)
        }
    }
}
