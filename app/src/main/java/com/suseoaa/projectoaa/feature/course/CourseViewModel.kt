package com.suseoaa.projectoaa.feature.course

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.CourseRepository
import com.suseoaa.projectoaa.core.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.core.data.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import javax.inject.Inject

// --- 数据类定义 ---
data class TermOption(
    val xnm: String,
    val xqm: String,
    val label: String
)

data class TimeSlotConfig(
    val sectionName: String,
    val startTime: String,
    val endTime: String,
    val type: SlotType,
    val weight: Float
)

enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

data class ScheduleLayoutItem(
    val course: CourseWithTimes,
    val time: ClassTimeEntity,
    val startNodeIndex: Int,
    val endNodeIndex: Int,
    val dayIndex: Int
)

data class CourseListUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

// --- 作息时间常量 ---
val DailySchedulePost2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.2f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.2f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.2f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_DINNER, 0.4f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.2f)
)

val DailySchedulePre2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.2f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.2f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.2f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_DINNER, 0.4f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.2f),
    TimeSlotConfig("12", "21:30", "22:15", SlotType.CLASS, 1.2f)
)

@HiltViewModel
class CourseListViewModel @Inject constructor(
    application: Application,
    private val schoolCourseRepository: SchoolCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val tokenManager: TokenManager
) : AndroidViewModel(application) {

    private val database = CourseDatabase.getInstance(application)
    private val localRepository = CourseRepository(database.courseDao())

    private val PREFS_NAME = "course_prefs"
    private val KEY_START_DATE = "semester_start_date"

    val savedAccounts: StateFlow<List<CourseAccountEntity>> = localRepository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAccount: StateFlow<CourseAccountEntity?> = combine(
        savedAccounts,
        tokenManager.currentStudentId
    ) { accounts, selectedId ->
        if (accounts.isEmpty()) null
        else accounts.find { it.studentId == selectedId } ?: accounts.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _termOptions = MutableStateFlow<List<TermOption>>(emptyList())
    val termOptions = _termOptions.asStateFlow()

    var selectedXnm by mutableStateOf("2024")
    var selectedXqm by mutableStateOf("3")

    var uiState by mutableStateOf(CourseListUiState())
        private set

    var currentDisplayWeek by mutableIntStateOf(1)
    var realCurrentWeek by mutableIntStateOf(1)
        private set

    private var _semesterStartDate =
        MutableStateFlow<LocalDate>(LocalDate.now().with(DayOfWeek.MONDAY))
    val semesterStartDate: StateFlow<LocalDate> = _semesterStartDate

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCourses: StateFlow<List<CourseWithTimes>> = combine(
        currentAccount.filterNotNull(),
        snapshotFlow { selectedXnm },
        snapshotFlow { selectedXqm }
    ) { account, xnm, xqm ->
        Triple(account.studentId, xnm, xqm)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        localRepository.getCourses(studentId, xnm, xqm)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekScheduleMap: StateFlow<Map<Int, List<CourseWithTimes>>> = allCourses
        .map { list ->
            (1..25).associateWith { week -> calculateCoursesForWeek(week, list) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val currentDailySchedule: StateFlow<List<TimeSlotConfig>> = snapshotFlow { selectedXnm }
        .map { getDailySchedule(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, DailySchedulePost2025)

    // 预计算所有周次的布局数据
    val weekLayoutMap: StateFlow<Map<Int, List<ScheduleLayoutItem>>> = combine(
        weekScheduleMap,
        currentDailySchedule
    ) { map, schedule ->
        map.mapValues { (_, courses) ->
            calculateLayoutItems(courses, schedule)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val currentWeekLayoutData: StateFlow<List<ScheduleLayoutItem>> = combine(
        weekLayoutMap,
        snapshotFlow { currentDisplayWeek }
    ) { map, week ->
        map[week] ?: emptyList()
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    init {
        loadSemesterStart()
        viewModelScope.launch {
            currentAccount.collect { account ->
                if (account != null) {
                    generateTermOptions(account.njdmId)
                    // 初始化时自动选中当前真实学期
                    val (realXnm, realXqm) = calculateCurrentRealTerm()
                    selectedXnm = realXnm
                    selectedXqm = realXqm
                }
            }
        }
    }

    fun switchUser(account: CourseAccountEntity) {
        viewModelScope.launch {
            tokenManager.saveCurrentStudentId(account.studentId)
        }
    }

    fun refreshSchedule() {
        val account = currentAccount.value
        if (account == null) {
            uiState = uiState.copy(errorMessage = "当前无账号")
            return
        }
        // 刷新时使用当前 UI 选中的学期
        fetchAndSaveCourseSchedule(account.studentId, account.password, selectedXnm, selectedXqm)
    }

    // 清除 UI 消息 (供界面调用)
    fun clearUiMessage() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }

    /**
     * 登录并保存课表
     * [自动策略] 如果 xnm/xqm 为 null，则自动计算离当前时间最近的学期
     */
    fun fetchAndSaveCourseSchedule(
        username: String,
        pass: String,
        xnm: String? = null,
        xqm: String? = null
    ) {
        viewModelScope.launch {
            //请求开始前，清除旧的错误/成功状态
            uiState = uiState.copy(
                isLoading = true,
                statusMessage = "正在登录...",
                errorMessage = null,
                successMessage = null
            )

            // 1. 确定目标学年学期：如果有传参则用参数，没有则自动计算当前最新学期
            val (targetXnm, targetXqm) = if (xnm != null && xqm != null) {
                xnm to xqm
            } else {
                calculateCurrentRealTerm()
            }

            val loginResult = schoolAuthRepository.login(username, pass)

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "登录失败"
                uiState =
                    uiState.copy(isLoading = false, errorMessage = errorMsg, statusMessage = null)
                return@launch
            }

            uiState = uiState.copy(statusMessage = "正在获取课表 ($targetXnm-$targetXqm)...")
            val courseResult = schoolCourseRepository.getCourseSchedule(targetXnm, targetXqm)

            courseResult.onSuccess { courseData ->
                uiState = uiState.copy(statusMessage = "正在保存...")
                withContext(Dispatchers.IO) {
                    localRepository.saveFromResponse(username, pass, courseData)
                }

                uiState = uiState.copy(statusMessage = "正在同步校历...")
                val startDateStr = schoolCourseRepository.fetchSemesterStart()
                if (startDateStr != null) {
                    try {
                        setSemesterStartDate(LocalDate.parse(startDateStr))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "更新成功",
                    statusMessage = null,
                    errorMessage = null // 确保清除错误
                )
                // 保存并切换到新账号
                tokenManager.saveCurrentStudentId(username)

                // 如果当前UI显示的不是刚刚抓取的学期，顺便更新UI状态
                selectedXnm = targetXnm
                selectedXqm = targetXqm

            }.onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "获取失败: ${e.message}",
                    statusMessage = null
                )
            }
        }
    }

    fun addCustomCourse(
        name: String, location: String, teacher: String,
        weekday: Int, startNode: Int, duration: Int, weeks: String
    ) {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            localRepository.saveCustomCourse(
                studentId = account.studentId,
                xnm = selectedXnm,
                xqm = selectedXqm,
                name = name,
                location = location,
                teacher = teacher,
                weekday = weekday.toString(),
                startNode = startNode,
                duration = duration,
                weeks = weeks
            )
            uiState = uiState.copy(successMessage = "自定义课程已添加")
        }
    }

    fun deleteAccount(account: CourseAccountEntity) {
        viewModelScope.launch {
            localRepository.deleteAccount(account.studentId)
        }
    }

    fun updateTermSelection(xnm: String, xqm: String) {
        selectedXnm = xnm
        selectedXqm = xqm
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        val today = LocalDate.now()
        val currentYear = today.year
        val currentMonth = today.monthValue
        return when (currentMonth) {
            in 2..7 -> (currentYear - 1).toString() to "12" // 例：2026年3月 -> 2025-2026学年 第2学期(12)
            1 -> (currentYear - 1).toString() to "3"        // 例：2026年1月 -> 2025-2026学年 第1学期(3)
            else -> currentYear.toString() to "3"           // 例：2025年9月 -> 2025-2026学年 第1学期(3)
        }
    }

    private fun getDailySchedule(yearStr: String): List<TimeSlotConfig> {
        val year = yearStr.toIntOrNull() ?: 2025
        return if (year >= 2025) DailySchedulePost2025 else DailySchedulePre2025
    }

    private fun calculateLayoutItems(
        courses: List<CourseWithTimes>,
        dailySchedule: List<TimeSlotConfig>
    ): List<ScheduleLayoutItem> {
        val layoutItems = mutableListOf<ScheduleLayoutItem>()

        val sectionIndexMap = dailySchedule.mapIndexedNotNull { index, slot ->
            if (slot.sectionName.isNotEmpty()) slot.sectionName to index else null
        }.toMap()

        courses.forEach { course ->
            course.times.forEach { time ->
                val dayIndex = parseWeekday(time.weekday) - 1
                if (dayIndex in 0..6) {
                    val (startPeriod, span) = parsePeriod(time.period)
                    val startIndex = sectionIndexMap[startPeriod.toString()] ?: -1
                    if (startIndex != -1) {
                        var spanCounter = 0
                        var endIndex = startIndex
                        while (spanCounter < span && endIndex < dailySchedule.size) {
                            if (dailySchedule[endIndex].type == SlotType.CLASS) {
                                spanCounter++
                            }
                            endIndex++
                        }
                        layoutItems.add(
                            ScheduleLayoutItem(
                                course = course,
                                time = time,
                                startNodeIndex = startIndex,
                                endNodeIndex = endIndex,
                                dayIndex = dayIndex
                            )
                        )
                    }
                }
            }
        }
        return layoutItems
    }

    private fun parseWeekday(day: String): Int = when {
        day.contains("一") || day == "1" -> 1
        day.contains("二") || day == "2" -> 2
        day.contains("三") || day == "3" -> 3
        day.contains("四") || day == "4" -> 4
        day.contains("五") || day == "5" -> 5
        day.contains("六") || day == "6" -> 6
        day.contains("日") || day == "7" -> 7
        else -> 1
    }

    private fun parsePeriod(period: String): Pair<Int, Int> {
        try {
            val clean = period.replace("节", "")
            val parts = clean.split("-")
            if (parts.size == 2) {
                val start = parts[0].toInt()
                val end = parts[1].toInt()
                return start to (end - start + 1)
            }
            clean.toIntOrNull()?.let { return it to 1 }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1 to 2
    }

    private fun calculateCoursesForWeek(
        week: Int,
        allData: List<CourseWithTimes>
    ): List<CourseWithTimes> {
        if (allData.isEmpty()) return emptyList()
        return allData.mapNotNull { courseWithTimes ->
            val validTimes = courseWithTimes.times.filter { time ->
                val weekBit = 1L shl (week - 1)
                (time.weeksMask and weekBit) != 0L
            }
            if (validTimes.isNotEmpty()) courseWithTimes.copy(times = validTimes) else null
        }
    }

    private fun generateTermOptions(startYearStr: String) {
        val startYear =
            startYearStr.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.YEAR) - 1)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val list = mutableListOf<TermOption>()
        for (y in startYear..currentYear + 1) {
            list.add(TermOption(y.toString(), "3", "$y-${y + 1} 第1学期"))
            list.add(TermOption(y.toString(), "12", "$y-${y + 1} 第2学期"))
        }
        _termOptions.value = list
    }

    private fun loadSemesterStart() {
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val epochDay = prefs.getLong(KEY_START_DATE, -1L)
        val today = LocalDate.now()
        val start =
            if (epochDay != -1L) LocalDate.ofEpochDay(epochDay) else today.with(DayOfWeek.MONDAY)
        _semesterStartDate.value = start
        val weeksBetween = ChronoUnit.WEEKS.between(start, today).toInt() + 1
        realCurrentWeek = weeksBetween
        currentDisplayWeek = weeksBetween.coerceIn(1, 25)
    }

    fun setSemesterStartDate(date: LocalDate) {
        val monday = date.with(DayOfWeek.MONDAY)
        viewModelScope.launch {
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putLong(KEY_START_DATE, monday.toEpochDay()) }
            _semesterStartDate.value = monday
            val today = LocalDate.now()
            realCurrentWeek = ChronoUnit.WEEKS.between(monday, today).toInt() + 1
            currentDisplayWeek = realCurrentWeek.coerceIn(1, 25)
        }
    }
}