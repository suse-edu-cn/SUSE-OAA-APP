package com.suseoaa.projectoaa.presentation.course

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.store.BackgroundPageIds
import com.suseoaa.projectoaa.shared.domain.course.CourseScheduleParser
import com.suseoaa.projectoaa.shared.domain.course.SemesterCalendar
import com.suseoaa.projectoaa.shared.domain.course.PeriodSpan
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.domain.model.course.PracticeCourseEntity
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.util.encodeBackgroundImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.domain.course.DailySchedulePost2025
import com.suseoaa.projectoaa.domain.course.SlotType
import com.suseoaa.projectoaa.domain.course.TimeSlotConfig
import com.suseoaa.projectoaa.domain.course.dailyScheduleFor
import com.suseoaa.projectoaa.shared.data.local.store.AppearanceStore
import com.suseoaa.projectoaa.shared.data.local.store.SemesterStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.domain.course.CourseOverlapCalculator
import com.suseoaa.projectoaa.domain.course.CourseOverlapDetail
import com.suseoaa.projectoaa.domain.course.CourseOverlapStatus
import com.suseoaa.projectoaa.domain.course.SectionSpan
import com.suseoaa.projectoaa.shared.util.SemesterNaming

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

class CourseViewModel(
    private val localRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolCourseRepository: SchoolCourseRepository,
    private val appearanceStore: AppearanceStore,
    private val semesterStore: SemesterStore,
    private val sessionStore: SessionStore
) : ViewModel() {

    private data class OverlapQueryConfig(
        val participantIds: List<String>,
        val xnm: String,
        val xqm: String
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
    val minWeek: Int get() = SemesterCalendar.minWeek(_hasWeekZero.value)

    /** 最大周次 */
    val maxWeek: Int get() = SemesterCalendar.MAX_WEEK

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
    val currentAccount: StateFlow<CourseAccountEntity?> = sessionStore.currentStudentId
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
        val min = SemesterCalendar.minWeek(hasZero)
        (min..SemesterCalendar.MAX_WEEK).associateWith { week -> calculateCoursesForWeek(week, list) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    /**
     * 整周实践课（实习等）。它们没有星期节次，不进课表格子，由界面单独展示。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val practiceCourses: StateFlow<List<PracticeCourseEntity>> = combine(
        currentAccount,
        selectedXnm,
        selectedXqm
    ) { account, xnm, xqm ->
        Triple(account?.studentId, xnm, xqm)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        if (studentId == null) flowOf(emptyList())
        else localRepository.getPracticeCourses(studentId, xnm, xqm)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val dailySchedule: StateFlow<List<TimeSlotConfig>> = selectedXnm
        .map { dailyScheduleFor(it) }
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

    val courseBackgroundImageBase64: StateFlow<String?> = appearanceStore.appBackgroundImagesFlow
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
            AppLog.d("[CourseVM] setupAutoRefresh: 已执行过，跳过")
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
                    AppLog.d("[CourseVM] setupAutoRefresh: 开始自动刷新课表")
                    // 直接调用完整的登录+获取流程（和手动刷新一样）
                    fetchAndSaveCourseSchedule(account.studentId, account.password, xnm, xqm)
                }
        }
    }

    private fun loadSemesterStart() {
        viewModelScope.launch {
            // 从DataStore加载开学日期和第0周标志
            val savedDate = semesterStore.getSemesterStartDate()
            _hasWeekZero.value = semesterStore.getSemesterHasWeekZero()
            if (savedDate != null) {
                try {
                    _semesterStartDate.value = LocalDate.parse(savedDate)
                } catch (e: Exception) {
                    // 解析失败使用默认值
                    AppLog.e("[Course] Failed to parse saved semester start date: $savedDate")
                }
            }
            updateRealCurrentWeek()
        }
    }

    private fun updateRealCurrentWeek() {
        _realCurrentWeek.value = SemesterCalendar.currentWeek(
            semesterStart = _semesterStartDate.value,
            today = today(),
            hasWeekZero = _hasWeekZero.value
        )
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
            appearanceStore.saveBackgroundImageForPages(encoded, setOf(BackgroundPageIds.COURSE))
            _uiState.value = _uiState.value.copy(successMessage = "课表背景图已更新")
        }
    }

    fun clearCourseBackgroundImage() {
        viewModelScope.launch {
            appearanceStore.clearBackgroundImageForPages(setOf(BackgroundPageIds.COURSE))
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
        AppLog.d("[CourseVM] refreshSchedule: studentId=${account.studentId}, password length=${account.password.length}, password hash=${account.password.hashCode()}")
        fetchAndSaveCourseSchedule(
            account.studentId,
            account.password,
            selectedXnm.value,
            selectedXqm.value
        )
    }

    fun switchUser(account: CourseAccountEntity) {
        viewModelScope.launch {
            sessionStore.saveCurrentStudentId(account.studentId)
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
            semesterStore.saveSemesterStartDate(date.toString())
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
            AppLog.d("[CourseVM] fetchAndSaveCourseSchedule: username=$username, password length=${password.length}, password hash=${password.hashCode()}")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusMessage = "正在登录教务系统...",
                errorMessage = null,
                successMessage = null
            )

            // 1. 登录
            AppLog.d("[CourseVM] 开始调用 login...")
            val loginResult = schoolAuthRepository.login(username, password)
            AppLog.e("[CourseVM] login 返回: isSuccess=${loginResult.isSuccess}, error=${loginResult.exceptionOrNull()?.message}")

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "教务系统登录失败"
                AppLog.e("[CourseVM] 登录失败: $errorMsg")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg,
                    statusMessage = null
                )
                return@launch
            }

            AppLog.d("[CourseVM] 登录成功，开始获取课表...")

            // 2. 获取课表
            _uiState.value = _uiState.value.copy(statusMessage = "正在获取课表 ($xnm-$xqm)...")
            val courseResult = schoolCourseRepository.getCourseSchedule(xnm, xqm)

            courseResult.onSuccess { courseData ->
                _uiState.value = _uiState.value.copy(statusMessage = "正在保存...")

                localRepository.saveFromResponse(username, password, courseData)

                // 切换到新导入的账号
                sessionStore.saveCurrentStudentId(username)

                // 3. 获取校历（开学日期和是否有第0周）
                _uiState.value = _uiState.value.copy(statusMessage = "正在同步校历...")
                try {
                    val calendarInfo = schoolCourseRepository.fetchSemesterStart()
                    if (calendarInfo != null) {
                        val parsedDate = LocalDate.parse(calendarInfo.startDate)
                        _hasWeekZero.value = calendarInfo.hasWeekZero
                        semesterStore.saveSemesterHasWeekZero(calendarInfo.hasWeekZero)
                        setSemesterStartDate(parsedDate)
                    }
                } catch (e: Exception) {
                    // 校历获取失败不影响整体流程
                    AppLog.e("[Course] Failed to fetch semester start: ${e.message}")
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

    private fun isWeekActive(week: Int, weeksStr: String, mask: Long): Boolean =
        CourseScheduleParser.isWeekActive(week, weeksStr, mask)

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
        val currentSpan = parseSectionSpan(item.time, studentId = item.course.course.studentId)
            ?: return CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)
        return CourseOverlapCalculator.detail(currentSpan, otherSpans)
    }

    private fun parseWeekday(weekday: String): Int =
        CourseScheduleParser.parseWeekday(weekday)

    private fun parsePeriod(period: String): PeriodSpan =
        CourseScheduleParser.parsePeriod(period)

    private fun generateTermOptions(njdmId: String) {
        val currentYear = com.suseoaa.projectoaa.shared.util.OaaClock.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year
        val startYear = njdmId.take(4).toIntOrNull() ?: (currentYear - 4)

        val options = mutableListOf<TermOption>()
        for (y in startYear..currentYear + 1) {
            options.add(TermOption(y.toString(), "3", SemesterNaming.full(startYear, y.toString(), "3")))
            options.add(TermOption(y.toString(), "12", SemesterNaming.full(startYear, y.toString(), "12")))
        }
        _termOptions.value = options.reversed()
    }

    private fun calculateCurrentRealTerm(): Pair<String, String> {
        return com.suseoaa.projectoaa.shared.util.getCurrentTerm()
    }
}
