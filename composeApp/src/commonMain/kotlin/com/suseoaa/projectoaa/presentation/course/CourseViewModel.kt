package com.suseoaa.projectoaa.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.ClassTimeEntity
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.model.CourseWithTimes
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolCourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

/**
 * 学期选项
 */
data class TermOption(
    val xnm: String,      // 学年码
    val xqm: String,      // 学期码: "3" = 第一学期, "12" = 第二学期
    val label: String     // 显示标签
)

/**
 * 时间段配置
 */
data class TimeSlotConfig(
    val sectionName: String,
    val startTime: String,
    val endTime: String,
    val type: SlotType,
    val weight: Float
)

enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

/**
 * 布局计算结果
 */
data class ScheduleLayoutItem(
    val course: CourseWithTimes,
    val time: ClassTimeEntity,
    val startNodeIndex: Int,
    val endNodeIndex: Int,
    val dayIndex: Int
)

/**
 * UI状态
 */
data class CourseListUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

/**
 * 2025年后的课程时间表
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
 * 2025年之前的课程时间表（12节课）
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

class CourseViewModel(
    private val localRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolCourseRepository: SchoolCourseRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ==================== 日期计算 ====================

    private fun getCurrentMonday(): LocalDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // ==================== 状态 ====================

    private val _semesterStartDate = MutableStateFlow(getCurrentMonday())
    val semesterStartDate: StateFlow<LocalDate> = _semesterStartDate.asStateFlow()

    private val _uiState = MutableStateFlow(CourseListUiState())
    val uiState: StateFlow<CourseListUiState> = _uiState.asStateFlow()

    private val _selectedXnm = MutableStateFlow("2024")
    val selectedXnm: StateFlow<String> = _selectedXnm.asStateFlow()

    private val _selectedXqm = MutableStateFlow("3")
    val selectedXqm: StateFlow<String> = _selectedXqm.asStateFlow()

    private val _currentDisplayWeek = MutableStateFlow(1)
    val currentDisplayWeek: StateFlow<Int> = _currentDisplayWeek.asStateFlow()

    // 真实当前周（用于高亮）
    private val _realCurrentWeek = MutableStateFlow(1)
    val realCurrentWeek: StateFlow<Int> = _realCurrentWeek.asStateFlow()

    private val _termOptions = MutableStateFlow<List<TermOption>>(emptyList())
    val termOptions: StateFlow<List<TermOption>> = _termOptions.asStateFlow()

    // 防止重复自动刷新
    private var hasAutoRefreshed = false

    // ==================== 账号管理（教务系统账号，与软件账号分开）====================

    val savedAccounts: StateFlow<List<CourseAccountEntity>> = localRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
        .flatMapLatest { selectedId ->
            savedAccounts.map { accounts ->
                if (accounts.isEmpty()) null
                else if (selectedId != null) accounts.find { it.studentId == selectedId }
                    ?: accounts.firstOrNull()
                else accounts.firstOrNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ==================== 课程数据 ====================

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCourses: StateFlow<List<CourseWithTimes>> = combine(
        currentAccount.filterNotNull(),
        selectedXnm,
        selectedXqm
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

    val dailySchedule: StateFlow<List<TimeSlotConfig>> = selectedXnm
        .map { getDailySchedule(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, DailySchedulePost2025)

    // 预计算每周的布局数据，供 Pager 使用
    val weekLayoutMap: StateFlow<Map<Int, List<ScheduleLayoutItem>>> = combine(
        weekScheduleMap,
        dailySchedule
    ) { weekMap, schedule ->
        weekMap.mapValues { (week, courses) ->
            calculateLayoutItems(week, courses, schedule)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        initializeData()
        loadSemesterStart()
        setupAutoRefresh()
    }

    private fun initializeData() {
        viewModelScope.launch {
            // 监听当前账号变化，自动刷新数据
            currentAccount
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.studentId == new.studentId }
                .collect { account ->
                    generateTermOptions(account.njdmId)
                    val (realXnm, realXqm) = calculateCurrentRealTerm()
                    _selectedXnm.value = realXnm
                    _selectedXqm.value = realXqm
                }
        }
    }

    /**
     * 设置自动刷新：进入软件后自动获取最新课表
     */
    private fun setupAutoRefresh() {
        // 防止重复执行
        if (hasAutoRefreshed) {
            println("[CourseVM] setupAutoRefresh: 已执行过，跳过")
            return
        }
        hasAutoRefreshed = true

        viewModelScope.launch {
            // 等待账号和学期数据都准备好
            combine(
                currentAccount.filterNotNull(),
                selectedXnm,
                selectedXqm
            ) { account, xnm, xqm -> Triple(account, xnm, xqm) }
                .first { (account, xnm, xqm) ->
                    xnm.isNotEmpty() && xqm.isNotEmpty() && account.password.isNotEmpty()
                }
                .let { (account, xnm, xqm) ->
                    // 等待足够时间确保所有网络组件完全初始化
                    kotlinx.coroutines.delay(2000)
                    println("[CourseVM] setupAutoRefresh: 开始自动刷新课表")
                    // 直接调用完整的登录+获取流程（和手动刷新一样）
                    fetchAndSaveCourseSchedule(account.studentId, account.password, xnm, xqm)
                }
        }
    }

    private fun loadSemesterStart() {
        viewModelScope.launch {
            // 仍DataStore加载开学日期
            val savedDate = tokenManager.getSemesterStartDate()
            if (savedDate != null) {
                try {
                    _semesterStartDate.value = LocalDate.parse(savedDate)
                } catch (e: Exception) {
                    // 解析失败使用默认值
                    println("[Course] Failed to parse saved semester start date: $savedDate")
                }
            }
            updateRealCurrentWeek()
        }
    }

    private fun updateRealCurrentWeek() {
        val start = _semesterStartDate.value
        val todayDate = today()
        val daysBetween = start.daysUntil(todayDate)
        val weekNum = (daysBetween / 7) + 1
        _realCurrentWeek.value = weekNum.coerceIn(1, 25)
        _currentDisplayWeek.value = _realCurrentWeek.value
    }

    // ==================== 公开方法 ====================

    fun setDisplayWeek(week: Int) {
        _currentDisplayWeek.value = week.coerceIn(1, 25)
    }

    fun selectTerm(xnm: String, xqm: String) {
        _selectedXnm.value = xnm
        _selectedXqm.value = xqm
    }

    fun refreshSchedule() {
        val account = currentAccount.value
        if (account == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先添加教务账号")
            return
        }
        println("[CourseVM] refreshSchedule: studentId=${account.studentId}, password length=${account.password.length}, password hash=${account.password.hashCode()}")
        fetchAndSaveCourseSchedule(
            account.studentId,
            account.password,
            selectedXnm.value,
            selectedXqm.value
        )
    }

    fun switchUser(account: CourseAccountEntity) {
        viewModelScope.launch {
            tokenManager.saveCurrentStudentId(account.studentId)
        }
    }

    /**
     * 删除教务账号
     */
    fun deleteAccount(account: CourseAccountEntity) {
        viewModelScope.launch {
            localRepository.deleteAccount(account.studentId)
            _uiState.value = _uiState.value.copy(successMessage = "已删除账号: ${account.name}")
        }
    }

    /**
     * 设置开学日期
     */
    fun setSemesterStartDate(date: LocalDate) {
        viewModelScope.launch {
            _semesterStartDate.value = date
            // 持久化到 DataStore
            tokenManager.saveSemesterStartDate(date.toString())
            updateRealCurrentWeek()
        }
    }

    /**
     * 添加自定义课程
     */
    fun addCustomCourse(
        courseName: String,
        location: String,
        teacher: String,
        weeks: String,
        dayOfWeek: Int,
        startNode: Int,
        duration: Int
    ) {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            localRepository.addCustomCourse(
                studentId = account.studentId,
                xnm = selectedXnm.value,
                xqm = selectedXqm.value,
                courseName = courseName,
                location = location,
                teacher = teacher,
                weeks = weeks,
                dayOfWeek = dayOfWeek,
                startNode = startNode,
                duration = duration
            )
            _uiState.value = _uiState.value.copy(successMessage = "添加成功: $courseName")
        }
    }

    /**
     * 导入课表（用于登录对话框）
     */
    fun fetchAndSaveCourseSchedule(
        username: String,
        password: String,
        xnm: String = selectedXnm.value,
        xqm: String = selectedXqm.value
    ) {
        viewModelScope.launch {
            println("[CourseVM] fetchAndSaveCourseSchedule: username=$username, password length=${password.length}, password hash=${password.hashCode()}")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusMessage = "正在登录教务系统...",
                errorMessage = null,
                successMessage = null
            )

            // 1. 登录
            println("[CourseVM] 开始调用 login...")
            val loginResult = schoolAuthRepository.login(username, password)
            println("[CourseVM] login 返回: isSuccess=${loginResult.isSuccess}, error=${loginResult.exceptionOrNull()?.message}")

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "教务系统登录失败"
                println("[CourseVM] 登录失败: $errorMsg")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg,
                    statusMessage = null
                )
                return@launch
            }

            println("[CourseVM] 登录成功，开始获取课表...")

            // 2. 获取课表
            _uiState.value = _uiState.value.copy(statusMessage = "正在获取课表 ($xnm-$xqm)...")
            val courseResult = schoolCourseRepository.getCourseSchedule(xnm, xqm)

            courseResult.onSuccess { courseData ->
                _uiState.value = _uiState.value.copy(statusMessage = "正在保存...")

                localRepository.saveFromResponse(username, password, courseData)

                // 切换到新导入的账号
                tokenManager.saveCurrentStudentId(username)

                // 3. 获取校历（开学日期）
                _uiState.value = _uiState.value.copy(statusMessage = "正在同步校历...")
                try {
                    val startDateStr = schoolCourseRepository.fetchSemesterStart()
                    if (startDateStr != null) {
                        val parsedDate = LocalDate.parse(startDateStr)
                        setSemesterStartDate(parsedDate)
                    }
                } catch (e: Exception) {
                    // 校历获取失败不影响整体流程
                    println("[Course] Failed to fetch semester start: ${e.message}")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "导入成功",
                    statusMessage = null,
                    errorMessage = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "获取课表失败: ${e.message}",
                    statusMessage = null
                )
            }
        }
    }

    fun clearUiMessage() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null,
            statusMessage = null
        )
    }

    /**
     * 删除课程
     */
    fun deleteCourse(courseName: String) {
        viewModelScope.launch {
            currentAccount.value?.let { account ->
                localRepository.deleteCourse(
                    studentId = account.studentId,
                    courseName = courseName,
                    xnm = selectedXnm.value,
                    xqm = selectedXqm.value,
                    isCustom = true // 目前只支持删除自定义课程
                )
                _uiState.value = _uiState.value.copy(successMessage = "已删除: $courseName")
            }
        }
    }

    // ==================== 私有方法 ====================

    private fun getDailySchedule(year: String): List<TimeSlotConfig> {
        return if (year >= "2025") DailySchedulePost2025 else DailySchedulePre2025
    }

    private fun calculateCoursesForWeek(
        week: Int,
        courses: List<CourseWithTimes>
    ): List<CourseWithTimes> {
        return courses.filter { courseWithTimes ->
            courseWithTimes.times.any { time ->
                isWeekActive(week, time.weeks, time.weeksMask)
            }
        }
    }

    private fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean {
        // 如果有掩码，优先使用掩码
        if (mask != 0L) {
            return (mask and (1L shl (week - 1))) != 0L
        }
        // 否则解析字符串
        return parseWeeksString(weeksStr, week)
    }

    /**
     * 解析周次字符串，如 "1-16周", "1,3,5周", "1-8,10-16周", "1-16周(单)", "2-16周(双)"
     */
    private fun parseWeeksString(weeksStr: String, targetWeek: Int): Boolean {
        if (weeksStr.isBlank()) return true

        // 检测整个字符串是否包含单双周标记
        val globalOddOnly = weeksStr.contains("单") && !weeksStr.contains("双")
        val globalEvenOnly = weeksStr.contains("双") && !weeksStr.contains("单")

        // 清理字符串
        val cleanStr = weeksStr
            .replace("周", "")
            .replace("(单)", "#ODD#")
            .replace("（单）", "#ODD#")
            .replace("(双)", "#EVEN#")
            .replace("（双）", "#EVEN#")
            .replace("单", "")
            .replace("双", "")
            .replace(" ", "")

        val parts = cleanStr.split(",")

        for (part in parts) {
            // 检查此部分是否有单双周标记
            val isOddOnly = part.contains("#ODD#") || (globalOddOnly && !part.contains("#EVEN#"))
            val isEvenOnly = part.contains("#EVEN#") || (globalEvenOnly && !part.contains("#ODD#"))

            val cleanPart = part.replace("#ODD#", "").replace("#EVEN#", "")

            if (cleanPart.contains("-")) {
                val range = cleanPart.split("-")
                if (range.size == 2) {
                    val start = range[0].toIntOrNull() ?: continue
                    val end = range[1].toIntOrNull() ?: continue
                    if (targetWeek in start..end) {
                        // 检查单双周是否匹配
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
                if (single == targetWeek) return true
            }
        }
        return false
    }

    /**
     * 布局计算：将课程映射到时间网格
     * @param week 当前周次，用于过滤不在该周的时间段
     */
    private fun calculateLayoutItems(
        week: Int,
        courses: List<CourseWithTimes>,
        schedule: List<TimeSlotConfig>
    ): List<ScheduleLayoutItem> {
        val result = mutableListOf<ScheduleLayoutItem>()

        // 构建节次索引映射
        val sectionIndexMap = schedule.mapIndexedNotNull { index, slot ->
            if (slot.sectionName.isNotEmpty() && slot.type == SlotType.CLASS) {
                slot.sectionName to index
            } else null
        }.toMap()

        courses.forEach { courseWithTimes ->
            courseWithTimes.times.forEach { time ->
                // 检查该时间段是否在当前周激活
                if (!isWeekActive(week, time.weeks, time.weeksMask)) return@forEach

                val dayIndex = parseWeekday(time.weekday) - 1
                if (dayIndex !in 0..6) return@forEach

                val (startPeriod, span) = parsePeriod(time.period)
                val startIndex = sectionIndexMap[startPeriod.toString()] ?: return@forEach

                // 计算结束索引
                var endIndex = startIndex
                var foundSpan = 1
                for (i in (startIndex + 1) until schedule.size) {
                    val slot = schedule[i]
                    if (slot.type == SlotType.CLASS && slot.sectionName.isNotEmpty()) {
                        foundSpan++
                        endIndex = i
                        if (foundSpan >= span) break
                    }
                }

                result.add(
                    ScheduleLayoutItem(
                        course = courseWithTimes,
                        time = time,
                        startNodeIndex = startIndex,
                        endNodeIndex = endIndex,
                        dayIndex = dayIndex
                    )
                )
            }
        }

        return result
    }

    /**
     * 解析星期字符串
     */
    private fun parseWeekday(weekday: String): Int {
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

    /**
     * 解析节次字符串，如 "1-2", "3-4节"
     * @return Pair(起始节, 跨度)
     */
    private fun parsePeriod(period: String): Pair<Int, Int> {
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

    private fun generateTermOptions(njdmId: String) {
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val startYear = njdmId.take(4).toIntOrNull() ?: (currentYear - 4)

        val options = mutableListOf<TermOption>()
        for (y in startYear..currentYear + 1) {
            options.add(TermOption(y.toString(), "3", "${y}-${y + 1}学年 第1学期"))
            options.add(TermOption(y.toString(), "12", "${y}-${y + 1}学年 第2学期"))
        }
        _termOptions.value = options.reversed()
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
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
}
