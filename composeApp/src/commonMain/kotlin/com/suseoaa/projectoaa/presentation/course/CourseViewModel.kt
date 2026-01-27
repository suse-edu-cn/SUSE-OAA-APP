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

// 删除原文件的 TermOption 定义，统一在这里重定义，避免冲突
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

class CourseViewModel(
    private val localRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolCourseRepository: SchoolCourseRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val KEY_START_DATE = "semester_start_date"
    
    // KMP日期处理: 获取当前周一的日期
    private fun getCurrentMonday(): LocalDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }

    private val _semesterStartDate = MutableStateFlow(getCurrentMonday())
    val semesterStartDate: StateFlow<LocalDate> = _semesterStartDate

    private val _uiState = MutableStateFlow(CourseListUiState())
    val uiState: StateFlow<CourseListUiState> = _uiState.asStateFlow()

    private val _selectedXnm = MutableStateFlow("2024")
    val selectedXnm: StateFlow<String> = _selectedXnm.asStateFlow()

    private val _selectedXqm = MutableStateFlow("3")
    val selectedXqm: StateFlow<String> = _selectedXqm.asStateFlow()

    private val _currentDisplayWeek = MutableStateFlow(1)
    val currentDisplayWeek: StateFlow<Int> = _currentDisplayWeek.asStateFlow()

    private val _termOptions = MutableStateFlow<List<TermOption>>(emptyList())
    val termOptions: StateFlow<List<TermOption>> = _termOptions.asStateFlow()

    // 账号管理
    val savedAccounts: StateFlow<List<CourseAccountEntity>> = localRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAccount: StateFlow<CourseAccountEntity?> = combine(
        savedAccounts,
        tokenManager.currentStudentId // 这里假设TokenManager有这个Flow
    ) { accounts, selectedId ->
        if (accounts.isEmpty()) null
        else accounts.find { it.studentId == selectedId } ?: accounts.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    val currentWeekLayoutData: StateFlow<List<ScheduleLayoutItem>> = combine(
        weekScheduleMap,
        dailySchedule,
        currentDisplayWeek
    ) { map, schedule, week ->
        val courses = map[week] ?: emptyList()
        calculateLayoutItems(courses, schedule)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            // 自动刷新逻辑
            currentAccount
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.studentId == new.studentId }
                .collect { account ->
                    generateTermOptions(account.njdmId)
                    val (realXnm, realXqm) = calculateCurrentRealTerm()
                    _selectedXnm.value = realXnm
                    _selectedXqm.value = realXqm
                    
                    if (account.password.isNotBlank()) {
                         fetchAndSaveCourseSchedule(account.studentId, account.password, realXnm, realXqm)
                    }
                }
        }
    }
    
    fun setDisplayWeek(week: Int) {
        _currentDisplayWeek.value = week
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
        fetchAndSaveCourseSchedule(account.studentId, account.password, selectedXnm.value, selectedXqm.value)
    }
    
    fun switchUser(account: CourseAccountEntity) {
        viewModelScope.launch {
            tokenManager.saveCurrentStudentId(account.studentId)
        }
    }

    private fun fetchAndSaveCourseSchedule(
        username: String,
        pass: String,
        xnm: String,
        xqm: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusMessage = "正在同步教务系统...",
                errorMessage = null,
                successMessage = null
            )

            // 1. 登录
            val loginResult = schoolAuthRepository.login(username, pass)
            
            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "教务系统登录失败"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg, statusMessage = null)
                return@launch
            }

            // 2. 获取课表
            _uiState.value = _uiState.value.copy(statusMessage = "正在获取课表 ($xnm-$xqm)...")
            val courseResult = schoolCourseRepository.getCourseSchedule(xnm, xqm)

            courseResult.onSuccess { courseData ->
                 _uiState.value = _uiState.value.copy(statusMessage = "正在保存...")
                 
                 localRepository.saveFromResponse(username, pass, courseData)

                 _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "更新成功",
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

    private fun getDailySchedule(year: String): List<TimeSlotConfig> {
        return if (year >= "2025") DailySchedulePost2025 else DailySchedulePost2025 // 简化处理
    }
    
    private fun calculateCoursesForWeek(week: Int, courses: List<CourseWithTimes>): List<CourseWithTimes> {
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
        // 否则解析字符串 (简化版，实际需要解析 "1-16周" 等格式)
        return true
    }

    private fun calculateLayoutItems(
        courses: List<CourseWithTimes>, 
        schedule: List<TimeSlotConfig>
    ): List<ScheduleLayoutItem> {
        // 核心布局算法，将课程时间映射到网格位置
        val result = mutableListOf<ScheduleLayoutItem>()
        // TODO: 实现完整布局计算算法
        return result
    }
    
    private fun generateTermOptions(njdmId: String) {
        // 根据年级代码生成学期选项
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val startYear = njdmId.take(4).toIntOrNull() ?: currentYear
        
        val options = mutableListOf<TermOption>()
        for (y in startYear..currentYear + 1) {
            options.add(TermOption(y.toString(), "3", "${y}-${y+1}-1"))
            options.add(TermOption(y.toString(), "12", "${y}-${y+1}-2"))
        }
        _termOptions.value = options.reversed()
    }
    
    private fun calculateCurrentRealTerm(): Pair<String, String> {
        // 简单实现，默认返回当前学期
        return "2024" to "3"
    }
}
