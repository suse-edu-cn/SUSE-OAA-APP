package com.suseoaa.projectoaa.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.suseoaa.projectoaa.R
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.util.getCurrentTerm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.core.context.GlobalContext

private const val COURSE_REMINDER_ACTION = "com.suseoaa.projectoaa.ACTION_COURSE_REMINDER"
private const val COURSE_REMINDER_REQUEST_CODE = 1001
private const val COURSE_REMINDER_TEST_REQUEST_CODE = 1002
private const val COURSE_REMINDER_CHANNEL_ID = "course_reminder_channel"
private const val COURSE_REMINDER_CHANNEL_NAME = "上课提醒"

object CourseReminderScheduler {
    fun scheduleNextReminder(context: Context) {
        runBlocking {
            val nextReminder = resolveNextReminder()

            if (nextReminder == null) {
                cancelReminder(context, COURSE_REMINDER_REQUEST_CODE)
                return@runBlocking
            }

            scheduleAlarm(context, nextReminder, COURSE_REMINDER_REQUEST_CODE)
        }
    }

    /**
     * 测试入口：启用后在应用启动约10秒后弹出“最近一门课”的提醒通知。
     */
    fun scheduleTestReminderAfter10Seconds(context: Context, enabled: Boolean) {
        if (!enabled) return

        runBlocking {
            val nextReminder = resolveNextReminder() ?: return@runBlocking
            val testReminder = nextReminder.copy(
                remindAt = Instant.fromEpochMilliseconds(
                    Clock.System.now().toEpochMilliseconds() + 10_000L
                )
            )
            scheduleAlarm(context, testReminder, COURSE_REMINDER_TEST_REQUEST_CODE)
        }
    }

    private suspend fun resolveNextReminder(): NextReminder? {
        val koin = GlobalContext.get()
        val tokenManager = koin.get<TokenManager>()
        val localCourseRepository = koin.get<LocalCourseRepository>()

        val studentId = tokenManager.currentStudentId.first()
        if (studentId.isNullOrBlank()) {
            return null
        }

        val (xnm, xqm) = getCurrentTerm()
        val courses = localCourseRepository.getCourses(studentId, xnm, xqm).first()
        if (courses.isEmpty()) {
            return null
        }

        val semesterStart = tokenManager.getSemesterStartDate()
            ?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            ?: defaultSemesterStartDate()

        val hasWeekZero = tokenManager.getSemesterHasWeekZero()
        return findNextReminder(
            now = Clock.System.now(),
            xnm = xnm,
            semesterStart = semesterStart,
            hasWeekZero = hasWeekZero,
            courses = courses
        )
    }

    private fun scheduleAlarm(context: Context, reminder: NextReminder, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseReminderReceiver::class.java)
            .setAction(COURSE_REMINDER_ACTION)
            .putExtra("courseName", reminder.courseName)
            .putExtra("location", reminder.location)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        val triggerAtMillis = reminder.remindAt.toEpochMilliseconds()

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelReminder(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseReminderReceiver::class.java).setAction(COURSE_REMINDER_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun findNextReminder(
        now: Instant,
        xnm: String,
        semesterStart: LocalDate,
        hasWeekZero: Boolean,
        courses: List<CourseWithTimes>
    ): NextReminder? {
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = now.toLocalDateTime(timeZone).date
        val minWeek = if (hasWeekZero) 0 else 1
        val daysBetween = nowDate.toEpochDays() - semesterStart.toEpochDays()
        val currentWeek = ((daysBetween / 7) + minWeek).coerceIn(minWeek, 25)
        val nowMillis = now.toEpochMilliseconds()

        var best: NextReminder? = null
        var bestMillis = Long.MAX_VALUE
        for (week in currentWeek..25) {
            for (course in courses) {
                for (time in course.times) {
                    if (!isWeekActive(week, time.weeks, time.weeksMask)) continue
                    val weekday = parseWeekday(time.weekday)
                    val startSection = parseStartSection(time.period)
                    val startTime = getSectionStartTime(xnm, startSection) ?: continue

                    val weekOffset = week - minWeek
                    val classDateEpochDay = semesterStart.toEpochDays() + weekOffset * 7 + (weekday - 1)
                    val classDate = LocalDate.fromEpochDays(classDateEpochDay)
                    val classStart = LocalDateTime(classDate, startTime).toInstant(timeZone)
                    val remindAtMillis = classStart.toEpochMilliseconds() - 10 * 60 * 1000L
                    val remindAt = Instant.fromEpochMilliseconds(remindAtMillis)

                    if (remindAtMillis <= nowMillis) continue

                    val candidate = NextReminder(
                        remindAt = remindAt,
                        courseName = course.course.courseName,
                        location = time.location.ifBlank { "地点待定" }
                    )
                    if (best == null || remindAtMillis < bestMillis) {
                        best = candidate
                        bestMillis = remindAtMillis
                    }
                }
            }
        }
        return best
    }

    private fun defaultSemesterStartDate(): LocalDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return LocalDate.fromEpochDays(today.toEpochDays() - today.dayOfWeek.ordinal)
    }

    private fun parseStartSection(period: String): Int {
        val clean = period.replace("节", "").trim()
        val start = clean.substringBefore('-').trim()
        return start.toIntOrNull() ?: 1
    }

    private fun parseWeekday(weekday: String): Int {
        return when {
            weekday.contains("一") || weekday == "1" -> 1
            weekday.contains("二") || weekday == "2" -> 2
            weekday.contains("三") || weekday == "3" -> 3
            weekday.contains("四") || weekday == "4" -> 4
            weekday.contains("五") || weekday == "5" -> 5
            weekday.contains("六") || weekday == "6" -> 6
            weekday.contains("日") || weekday.contains("天") || weekday == "7" -> 7
            else -> weekday.toIntOrNull()?.coerceIn(1, 7) ?: 1
        }
    }

    private fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean {
        if (weeksStr.isNotBlank()) {
            return parseWeeksString(weeksStr, week)
        }
        if (mask != 0L && week > 0) {
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
                    if (targetWeek in start..end && parityMatches(targetWeek, isOddOnly, isEvenOnly)) {
                        return true
                    }
                }
            } else {
                val single = cleanPart.toIntOrNull()
                if (single == targetWeek && parityMatches(targetWeek, isOddOnly, isEvenOnly)) {
                    return true
                }
            }
        }
        return false
    }

    private fun parityMatches(week: Int, oddOnly: Boolean, evenOnly: Boolean): Boolean {
        return when {
            oddOnly -> week % 2 == 1
            evenOnly -> week % 2 == 0
            else -> true
        }
    }

    private fun getSectionStartTime(xnm: String, section: Int): LocalTime? {
        val schedulePost2025 = mapOf(
            1 to LocalTime(8, 30),
            2 to LocalTime(9, 20),
            3 to LocalTime(10, 25),
            4 to LocalTime(11, 15),
            5 to LocalTime(14, 0),
            6 to LocalTime(14, 50),
            7 to LocalTime(15, 55),
            8 to LocalTime(16, 45),
            9 to LocalTime(19, 0),
            10 to LocalTime(19, 50),
            11 to LocalTime(20, 40)
        )
        val schedulePre2025 = schedulePost2025 + (12 to LocalTime(21, 30))
        val mapping = if (xnm >= "2025") schedulePost2025 else schedulePre2025
        return mapping[section]
    }
}

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != COURSE_REMINDER_ACTION) return

        val courseName = intent.getStringExtra("courseName").orEmpty().ifBlank { "未知课程" }
        val location = intent.getStringExtra("location").orEmpty().ifBlank { "地点待定" }
        val message = "十分钟后上课 $courseName：$location"

        createChannelIfNeeded(context)

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val appIconResId = context.applicationInfo.icon
            val notification = NotificationCompat.Builder(context, COURSE_REMINDER_CHANNEL_ID)
                .setSmallIcon(appIconResId)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notification
            )
        }

        CourseReminderScheduler.scheduleNextReminder(context)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(COURSE_REMINDER_CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            COURSE_REMINDER_CHANNEL_ID,
            COURSE_REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "课程开课前10分钟提醒"
        }
        manager.createNotificationChannel(channel)
    }
}

class CourseReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                CourseReminderScheduler.scheduleNextReminder(context)
            }
        }
    }
}

private data class NextReminder(
    val remindAt: Instant,
    val courseName: String,
    val location: String
)
