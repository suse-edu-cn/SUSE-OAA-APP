package com.suseoaa.projectoaa.presentation.course

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.BackgroundPageIds
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.util.encodeBackgroundImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

/**
 * 学期选项
 */
@Immutable
data class TermOption(
    val xnm: String,      // 学年码
    val xqm: String,      // 学期码: "3" = 第一学期, "12" = 第二学期
    val label: String     // 显示标签
)

/**
 * 时间段配置
 */
@Immutable
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
@Immutable
data class ScheduleLayoutItem(
    val course: CourseWithTimes,
    val time: ClassTimeEntity,
    val startNodeIndex: Int,
    val endNodeIndex: Int,
    val dayIndex: Int
)

fun buildScheduleLayoutOverlapKey(item: ScheduleLayoutItem): String {
    val time = item.time
    val course = item.course.course
    return listOf(
        course.studentId,
        course.courseName,
        time.uniqueId.toString(),
        time.weekday,
        time.period,
        item.startNodeIndex.toString(),
        item.endNodeIndex.toString()
    ).joinToString("|")
}

enum class CourseOverlapStatus {
    NO_OVERLAP,
    OVERLAP,
    PARTIAL_OVERLAP
}

@Immutable
data class CourseOverlapDetail(
    val status: CourseOverlapStatus,
    val overlappedAccounts: List<String> = emptyList(),
    val overlappedCourses: List<String> = emptyList()
)

/**
 * UI状态
 */
@Immutable
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

    private data class OverlapQueryConfig(
        val participantIds: List<String>,
        val xnm: String,
        val xqm: String
    )

    private data class SectionSpan(
        val studentId: String,
        val dayIndex: Int,
        val startSection: Int,
        val endSection: Int,
        val accountName: String,
        val courseName: String
    )

    // ==================== 日期计算 ====================

    private fun getCurrentMonday(): LocalDate {
        val today = com.suseoaa.projectoaa.shared.util.OaaClock.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        return today.minus(today.dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
    }

    private fun today(): LocalDate = com.suseoaa.projectoaa.shared.util.OaaClock.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date

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

    // 是否存在第0周（由教务系统校历决定）
    private val _hasWeekZero = MutableStateFlow(false)
    val hasWeekZero: StateFlow<Boolean> = _hasWeekZero.asStateFlow()

    /** 最小周次：有第0周时为0，否则为1 */
    val minWeek: Int get() = if (_hasWeekZero.value) 0 else 1

    /** 最大周次 */
    val maxWeek: Int get() = if (_hasWeekZero.value) 25 else 25

    /** 总周数 */
    val totalWeeks: Int get() = maxWeek - minWeek + 1

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

    private val _overlapSelectedAccountIds = MutableStateFlow<Set<String>?>(null)
    val overlapSelectedAccountIds: StateFlow<Set<String>> = combine(
        _overlapSelectedAccountIds,
        currentAccount
    ) { selected, current ->
        selected ?: current?.studentId?.let { setOf(it) } ?: emptySet()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    // ==================== 课程数据 ====================

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overlapCoursesByAccount: StateFlow<Map<String, List<CourseWithTimes>>> = combine(
        savedAccounts,
        currentAccount,
        overlapSelectedAccountIds,
        selectedXnm,
        selectedXqm
    ) { accounts, current, selectedIds, xnm, xqm ->
        val normalizedIds = normalizeOverlapAccountIds(
            selectedIds = selectedIds,
            availableAccountIds = accounts.map { it.studentId }.toSet(),
            currentStudentId = current?.studentId
        ).toList()
        OverlapQueryConfig(participantIds = normalizedIds, xnm = xnm, xqm = xqm)
    }.flatMapLatest { query ->
        if (query.participantIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val courseFlows = query.participantIds.map { studentId ->
                localRepository.getCourses(studentId, query.xnm, query.xqm)
            }
            combine(courseFlows) { coursesByAccount ->
                query.participantIds.zip(coursesByAccount).associate { (studentId, courses) ->
                    studentId to courses
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())


    @OptIn(ExperimentalCoroutinesApi::class)
    val activeQueryCount: StateFlow<Int> = overlapCoursesByAccount
        .map { it.keys.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCourses: StateFlow<List<CourseWithTimes>> = overlapCoursesByAccount
        .map { map ->
            map.values.flatten().distinctBy {
                "${it.course.studentId}_${it.course.courseName}_${it.times.joinToString { t -> t.uniqueId.toString() }}"
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekScheduleMap: StateFlow<Map<Int, List<CourseWithTimes>>> = combine(
        allCourses,
        _hasWeekZero
    ) { list, hasZero ->
        val min = if (hasZero) 0 else 1
        (min..25).associateWith { week -> calculateCoursesForWeek(week, list) }
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


    val overlapDetailByWeek: StateFlow<Map<Int, Map<String, CourseOverlapDetail>>> = combine(
        weekLayoutMap,
        overlapCoursesByAccount,
        currentAccount,
        savedAccounts,
        _hasWeekZero
    ) { currentWeekLayouts, coursesByAccount, current, accounts, hasZero ->
        val currentStudentId = current?.studentId ?: return@combine emptyMap()
        val min = if (hasZero) 0 else 1
        val weekRange = min..maxWeek
        val accountNameById = accounts.associate { account ->
            account.studentId to account.name.ifBlank { account.studentId }
        }

        val otherSpansByWeek = buildOtherAccountSpansByWeek(
            weekRange = weekRange,
            currentStudentId = currentStudentId,
            coursesByAccount = coursesByAccount,
            accountNameById = accountNameById
        )

        weekRange.associateWith { week ->
            val weekItems = currentWeekLayouts[week].orEmpty()
            val otherSpans = otherSpansByWeek[week].orEmpty()
            weekItems.associate { item ->
                buildScheduleLayoutOverlapKey(item) to calculateOverlapDetail(item, otherSpans)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val overlapStatusByWeek: StateFlow<Map<Int, Map<String, CourseOverlapStatus>>> =
        overlapDetailByWeek
            .map { detailByWeek ->
                detailByWeek.mapValues { (_, detailByKey) ->
                    detailByKey.mapValues { (_, detail) -> detail.status }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val courseBackgroundImageBase64: StateFlow<String?> = tokenManager.appBackgroundImagesFlow
        .map { images -> images[BackgroundPageIds.COURSE] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        initializeData()
        loadSemesterStart()
        setupAutoRefresh()
        observeOverlapSelectionState()
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
            // 从DataStore加载开学日期和第0周标志
            val savedDate = tokenManager.getSemesterStartDate()
            _hasWeekZero.value = tokenManager.getSemesterHasWeekZero()
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
        val weekNum = (daysBetween / 7) + minWeek
        _realCurrentWeek.value = weekNum.coerceIn(minWeek, maxWeek)
        _currentDisplayWeek.value = _realCurrentWeek.value
    }

    // ==================== 公开方法 ====================

    fun setDisplayWeek(week: Int) {
        _currentDisplayWeek.value = week.coerceIn(minWeek, maxWeek)
    }

    fun setOverlapSelectedAccountIds(selectedIds: Set<String>) {
        val normalized = normalizeOverlapAccountIds(
            selectedIds = selectedIds,
            availableAccountIds = savedAccounts.value.map { it.studentId }.toSet(),
            currentStudentId = currentAccount.value?.studentId
        )
        _overlapSelectedAccountIds.value = normalized
    }

    fun saveCourseBackgroundImage(imageData: ByteArray) {
        val encoded = encodeBackgroundImage(imageData)
        if (encoded == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "图片过大，请选择更小图片后重试")
            return
        }

        viewModelScope.launch {
            tokenManager.saveBackgroundImageForPages(encoded, setOf(BackgroundPageIds.COURSE))
            _uiState.value = _uiState.value.copy(successMessage = "课表背景图已更新")
        }
    }

    fun clearCourseBackgroundImage() {
        viewModelScope.launch {
            tokenManager.clearBackgroundImageForPages(setOf(BackgroundPageIds.COURSE))
            _uiState.value = _uiState.value.copy(successMessage = "课表背景图已清除")
        }
    }

    /**
     * 同步系统日期对应的当前周（用于前后台切换后刷新）
     */
    fun syncCurrentWeek() {
        updateRealCurrentWeek()
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

                // 3. 获取校历（开学日期和是否有第0周）
                _uiState.value = _uiState.value.copy(statusMessage = "正在同步校历...")
                try {
                    val calendarInfo = schoolCourseRepository.fetchSemesterStart()
                    if (calendarInfo != null) {
                        val parsedDate = LocalDate.parse(calendarInfo.startDate)
                        _hasWeekZero.value = calendarInfo.hasWeekZero
                        tokenManager.saveSemesterHasWeekZero(calendarInfo.hasWeekZero)
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
        // 优先按原始周次字符串解析，避免掩码在“单双周 + 普通周次”混合场景下误判
        if (weeksStr.isNotBlank()) {
            return parseWeeksString(weeksStr, week)
        }
        // 周次字符串为空时再使用掩码
        if (mask != 0L) {
            return (mask and (1L shl (week - 1))) != 0L
        }
        return true
    }

    /**
     * 解析周次字符串，如 "1-16周", "1,3,5周", "1-8,10-16周", "1-16周(单)", "2-16周(双)"
     */
    private fun parseWeeksString(weeksStr: String, targetWeek: Int): Boolean {
        if (weeksStr.isBlank()) return true

        // 清理字符串
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
        // 仅当所有分段都没有显式单双周标记时，才使用全局单双周兜底
        val globalOddOnly =
            !hasSegmentParityTag && weeksStr.contains("单") && !weeksStr.contains("双")
        val globalEvenOnly =
            !hasSegmentParityTag && weeksStr.contains("双") && !weeksStr.contains("单")

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

    private fun observeOverlapSelectionState() {
        viewModelScope.launch {
            combine(
                savedAccounts,
                currentAccount,
                _overlapSelectedAccountIds
            ) { accounts, current, selected ->
                Triple(accounts, current?.studentId, selected)
            }.collect { (accounts, currentStudentId, selected) ->
                if (selected == null) return@collect
                val normalized = normalizeOverlapAccountIds(
                    selectedIds = selected,
                    availableAccountIds = accounts.map { it.studentId }.toSet(),
                    currentStudentId = currentStudentId
                )
                if (normalized != selected) {
                    _overlapSelectedAccountIds.value = normalized
                }
            }
        }
    }

    private fun normalizeOverlapAccountIds(
        selectedIds: Set<String>,
        availableAccountIds: Set<String>,
        currentStudentId: String?
    ): Set<String> {
        if (availableAccountIds.isEmpty()) return emptySet()

        return selectedIds.filterTo(linkedSetOf()) { it in availableAccountIds }
    }

    private fun buildOtherAccountSpansByWeek(
        weekRange: IntRange,
        currentStudentId: String,
        coursesByAccount: Map<String, List<CourseWithTimes>>,
        accountNameById: Map<String, String>
    ): Map<Int, List<SectionSpan>> {
        val spansByWeek = weekRange.associateWith { mutableListOf<SectionSpan>() }

        coursesByAccount.forEach { (studentId, courses) ->
            val accountName = accountNameById[studentId] ?: studentId

            courses.forEach { course ->
                course.times.forEach { time ->
                    val span = parseSectionSpan(
                        time = time,
                        studentId = studentId,
                        accountName = accountName,
                        courseName = course.course.courseName
                    ) ?: return@forEach
                    weekRange.forEach { week ->
                        if (isWeekActive(week, time.weeks, time.weeksMask)) {
                            spansByWeek[week]?.add(span)
                        }
                    }
                }
            }
        }

        return spansByWeek.mapValues { (_, spans) -> spans.toList() }
    }

    private fun parseSectionSpan(
        time: ClassTimeEntity,
        studentId: String = "",
        accountName: String = "",
        courseName: String = ""
    ): SectionSpan? {
        val dayIndex = parseWeekday(time.weekday) - 1
        if (dayIndex !in 0..6) return null

        val (startSection, span) = parsePeriod(time.period)
        val endSection = startSection + span - 1
        return SectionSpan(
            studentId = studentId,
            dayIndex = dayIndex,
            startSection = startSection,
            endSection = endSection,
            accountName = accountName,
            courseName = courseName
        )
    }

    private fun calculateOverlapDetail(
        item: ScheduleLayoutItem,
        otherSpans: List<SectionSpan>
    ): CourseOverlapDetail {
        val currentStudentId = item.course.course.studentId
        val currentSpan = parseSectionSpan(item.time)
            ?: return CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)

        val overlaps = otherSpans.filter { other ->
            other.studentId != currentStudentId &&
                    other.dayIndex == currentSpan.dayIndex &&
                    other.startSection <= currentSpan.endSection &&
                    other.endSection >= currentSpan.startSection
        }

        if (overlaps.isEmpty()) {
            return CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)
        }

        val hasExactOverlap = overlaps.any { other ->
            other.startSection == currentSpan.startSection &&
                    other.endSection == currentSpan.endSection
        }

        val status = if (hasExactOverlap) {
            CourseOverlapStatus.OVERLAP
        } else {
            CourseOverlapStatus.PARTIAL_OVERLAP
        }

        val accountNames = overlaps
            .mapNotNull { other -> other.accountName.ifBlank { null } }
            .distinct()
            .sorted()

        val courseNames = overlaps
            .map { other ->
                if (other.accountName.isBlank()) {
                    other.courseName
                } else {
                    "${other.courseName}（${other.accountName}）"
                }
            }
            .filter { value -> value.isNotBlank() }
            .distinct()
            .sorted()

        return CourseOverlapDetail(
            status = status,
            overlappedAccounts = accountNames,
            overlappedCourses = courseNames
        )
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
        val currentYear = com.suseoaa.projectoaa.shared.util.OaaClock.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year
        val startYear = njdmId.take(4).toIntOrNull() ?: (currentYear - 4)

        val options = mutableListOf<TermOption>()
        for (y in startYear..currentYear + 1) {
            options.add(TermOption(y.toString(), "3", "${y}-${y + 1}学年 第1学期"))
            options.add(TermOption(y.toString(), "12", "${y}-${y + 1}学年 第2学期"))
        }
        _termOptions.value = options.reversed()
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        return com.suseoaa.projectoaa.shared.util.getCurrentTerm()
    }
}
