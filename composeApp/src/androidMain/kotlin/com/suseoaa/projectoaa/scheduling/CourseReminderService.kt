package com.suseoaa.projectoaa.scheduling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.suseoaa.projectoaa.presentation.course.DailySchedulePost2025
import com.suseoaa.projectoaa.presentation.course.DailySchedulePre2025
import com.suseoaa.projectoaa.presentation.course.SlotType
import com.suseoaa.projectoaa.presentation.course.TimeSlotConfig
import com.suseoaa.projectoaa.presentation.checkin.ScheduledCheckinManager
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

class CourseReminderService : Service(), KoinComponent {

    private val localCourseRepository: LocalCourseRepository by inject()
    private val tokenManager: TokenManager by inject()
    private val scheduledCheckinManager: ScheduledCheckinManager by inject()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val NOTIFICATION_ID = 200605
    private val POPUP_NOTIFICATION_ID = 200606
    private val CHANNEL_ID_PERSISTENT = "course_reminder_persistent"
    private val CHANNEL_ID_POPUP = "course_reminder_popup"
    
    // 防止同一2分钟窗口内重复触发652签到
    private var lastAuxTriggerKey: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createPersistentNotification("正在获取课程信息...", "请稍候")
        startForeground(NOTIFICATION_ID, notification)

        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    checkAndNotify()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1.minutes)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                "常驻课表提醒",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "在通知栏显示今天剩余的课程和下一节课信息"
                setShowBadge(false)
            }
            
            val popupChannel = NotificationChannel(
                CHANNEL_ID_POPUP,
                "上课提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "上课前15分钟的弹窗提醒"
                enableVibration(true)
            }

            manager.createNotificationChannel(persistentChannel)
            manager.createNotificationChannel(popupChannel)
        }
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPersistentNotification(line1: String, line2: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(line1)
            .setContentText(line2)
            .setContentIntent(getMainActivityPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showPopupNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_POPUP)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(getMainActivityPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(getMainActivityPendingIntent(), true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(POPUP_NOTIFICATION_ID, notification)
    }

    private fun updatePersistentNotification(line1: String, line2: String) {
        val notification = createPersistentNotification(line1, line2)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun getCurrentTerm(): Pair<String, String> {
        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val month = now.monthNumber
        val year = now.year
        return if (month >= 8) {
            Pair(year.toString(), "3")
        } else {
            Pair((year - 1).toString(), "12")
        }
    }

    private fun getDailySchedule(year: String): List<TimeSlotConfig> {
        return if (year >= "2025") DailySchedulePost2025 else DailySchedulePre2025
    }

    private fun parsePeriod(period: String): Pair<Int, Int> {
        val parts = period.split("-")
        val start = parts.firstOrNull()?.toIntOrNull() ?: 1
        val end = parts.lastOrNull()?.toIntOrNull() ?: start
        val span = end - start + 1
        return Pair(start, span)
    }

    private fun parseWeekday(weekday: String): Int {
        return when (weekday) {
            "1", "一" -> 1
            "2", "二" -> 2
            "3", "三" -> 3
            "4", "四" -> 4
            "5", "五" -> 5
            "6", "六" -> 6
            "7", "日" -> 7
            else -> 1
        }
    }

    private fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean {
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
            .replace("(单)", "#ODD#").replace("（单）", "#ODD#")
            .replace("(双)", "#EVEN#").replace("（双）", "#EVEN#")
            .replace("，", ",").replace("；", ",").replace(";", ",")
            .replace("单", "").replace("双", "").replace(" ", "")

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

    private suspend fun checkAndNotify() {
        val studentId = tokenManager.currentStudentId.firstOrNull() ?: return
        val savedDateStr = tokenManager.getSemesterStartDate() ?: return
        val hasWeekZero = tokenManager.getSemesterHasWeekZero()
        
        val startDate = try {
            LocalDate.parse(savedDateStr)
        } catch (e: Exception) {
            return
        }

        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayDate = now.date
        val currentTime = now.time

        val daysBetween = startDate.daysUntil(todayDate)
        val minWeek = if (hasWeekZero) 0 else 1
        val currentWeek = (daysBetween / 7) + minWeek
        val currentDayOfWeek = todayDate.dayOfWeek.ordinal + 1

        val (xnm, xqm) = getCurrentTerm()
        val coursesWithTimes = localCourseRepository.getCourses(studentId, xnm, xqm).firstOrNull() ?: emptyList()

        val schedule = getDailySchedule(xnm)
        val sectionIndexMap = schedule.mapIndexedNotNull { index, slot ->
            if (slot.sectionName.isNotEmpty() && slot.type == SlotType.CLASS) {
                slot.sectionName to index
            } else null
        }.toMap()

        // 提取今天的课
        data class ClassItem(val courseName: String, val timeEntity: ClassTimeEntity, val startLocalTime: LocalTime)
        
        val todayClasses = mutableListOf<ClassItem>()

        for (courseData in coursesWithTimes) {
            for (time in courseData.times) {
                if (!isWeekActive(currentWeek, time.weeks, time.weeksMask)) continue
                if (parseWeekday(time.weekday) != currentDayOfWeek) continue

                val (startNode, _) = parsePeriod(time.period)
                val startIndex = sectionIndexMap[startNode.toString()] ?: continue
                val slotConfig = schedule[startIndex]
                
                try {
                    val timeStr = slotConfig.startTime
                    val parts = timeStr.split(":")
                    if (parts.size >= 2) {
                        val startLocalTime = LocalTime(parts[0].toInt(), parts[1].toInt())
                        todayClasses.add(ClassItem(courseData.course.courseName, time, startLocalTime))
                    }
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
        }

        todayClasses.sortBy { it.startLocalTime }

        val upcomingClasses = todayClasses.filter { it.startLocalTime > currentTime }
        val remainingCount = upcomingClasses.size

        if (remainingCount == 0) {
            updatePersistentNotification("今天没有更多课了", "好好休息吧！")
            return
        }

        val nextClass = upcomingClasses.first()
        val nextClassRoom = nextClass.timeEntity.location
        val nextClassTime = nextClass.startLocalTime

        updatePersistentNotification(
            "今天还有 ${remainingCount} 节课",
            "下一节课在 ${nextClassTime.hour}:${nextClassTime.minute.toString().padStart(2, '0')}，${nextClassRoom.ifEmpty { "未知教室" }}"
        )

        // 判断是否需要弹出 15 分钟提醒
        // 计算时间差：由于我们每分钟执行一次，只要当前时间 + 15 分钟等于上课时间，就提醒
        val timeUntilNextClass = (nextClassTime.toSecondOfDay() - currentTime.toSecondOfDay()) / 60
        if (timeUntilNextClass == 15) {
            showPopupNotification(
                "即将上课: ${nextClass.courseName}",
                "距离上课还有15分钟，地点: ${nextClassRoom.ifEmpty { "未知" }}"
            )
        }
        
        // ================= 保活强化：辅助触发652自动签到 =================
        try {
            val config = scheduledCheckinManager.getConfig()
            if (config.enabled && config.targetAccountIds.isNotEmpty()) {
                val nowTime = OaaClock.now().toLocalDateTime(TimeZone.of("Asia/Shanghai"))
                val currentSeconds = nowTime.hour * 3600 + nowTime.minute * 60 + nowTime.second
                val scheduledSeconds = config.scheduledHour * 3600 + config.scheduledMinute * 60 + config.scheduledSecond
                
                // 如果当前时间正好在设定的签到时间之后（允许2分钟延迟），且今天还没签到
                if (currentSeconds in scheduledSeconds..(scheduledSeconds + 120)) {
                    if (!scheduledCheckinManager.hasAlreadyRunToday(config)) {
                        // 使用标志避免在同一2分钟窗口内重复触发
                        val triggerKey = "${nowTime.date}_${config.scheduledHour}_${config.scheduledMinute}"
                        if (lastAuxTriggerKey != triggerKey) {
                            lastAuxTriggerKey = triggerKey
                            println("[CourseReminderService] 保活服务检测到签到时间，主动拉起 CheckinAlarmReceiver")
                            val intent = Intent(this, CheckinAlarmReceiver::class.java)
                            intent.action = "com.suseoaa.projectoaa.CHECKIN_ALARM"
                            sendBroadcast(intent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("[CourseReminderService] 辅助触发签到异常: ${e.message}")
        }
    }
}
