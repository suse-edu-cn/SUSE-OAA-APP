package com.suseoaa.projectoaa.feature.course

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import androidx.core.content.edit
import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes

data class TermOption(
    val xnm: String, // 2024
    val xqm: String, // 3 或 12
    val label: String // "2024-2025 第1学期"
)

class CourseListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CourseDatabase.getInstance(application)
    private val repository = CourseRepository(database.courseDao())
    private val PREFS_NAME = "course_prefs"
    private val KEY_START_DATE = "semester_start_date"

    // === 状态管理 ===

    private val _currentAccount = MutableStateFlow<CourseAccountEntity?>(null)
    val currentAccount = _currentAccount.asStateFlow()

    val savedAccounts: StateFlow<List<CourseAccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // === 课程数据流 ===
    @OptIn(ExperimentalCoroutinesApi::class)
    val allCourses: StateFlow<List<CourseWithTimes>> = combine(
        _currentAccount.filterNotNull(),
        snapshotFlow { selectedXnm },
        snapshotFlow { selectedXqm }
    ) { account, xnm, xqm ->
        Triple(account.studentId, xnm, xqm)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        repository.getCourses(studentId, xnm, xqm)
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekScheduleMap: StateFlow<Map<Int, List<CourseWithTimes>>> = allCourses
        .map { list ->
            (1..25).associateWith { week ->
                calculateCoursesForWeek(week, list)
            }
        }
        // 在后台计算
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())


    init {
        loadSemesterStart()
        viewModelScope.launch {
            savedAccounts.collect { accounts ->
                if (_currentAccount.value == null && accounts.isNotEmpty()) {
                    switchUser(accounts.first())
                }
            }
        }
    }

    // === 业务逻辑 ===

    fun switchUser(account: CourseAccountEntity, preserveSelection: Boolean = false) {
        _currentAccount.value = account
        generateTermOptions(account.njdmId)

        if (!preserveSelection) {
            selectCurrentRealTerm()
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

    private fun selectCurrentRealTerm() {
        val today = LocalDate.now()
        val currentYear = today.year
        val currentMonth = today.monthValue

        val (targetXnm, targetXqm) = when (currentMonth) {
            in 2..7 -> (currentYear - 1).toString() to "12"
            1 -> (currentYear - 1).toString() to "3"
            else -> currentYear.toString() to "3"
        }

        val options = _termOptions.value
        val match = options.find { it.xnm == targetXnm && it.xqm == targetXqm }

        if (match != null) {
            selectedXnm = match.xnm
            selectedXqm = match.xqm
        } else if (options.isNotEmpty()) {
            val last = options.last()
            selectedXnm = last.xnm
            selectedXqm = last.xqm
        }
    }

    fun deleteAccount(account: CourseAccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account.studentId)
            if (_currentAccount.value == account) {
                val remaining = savedAccounts.value.filter { it.studentId != account.studentId }
                if (remaining.isNotEmpty()) {
                    switchUser(remaining.first())
                } else {
                    _currentAccount.value = null
                }
            }
        }
    }

    fun updateTermSelection(xnm: String, xqm: String) {
        selectedXnm = xnm
        selectedXqm = xqm
    }

    fun refreshSchedule() {
        val account = _currentAccount.value
        if (account == null) {
            uiState = uiState.copy(errorMessage = "当前无账号")
            return
        }
        fetchAndSaveCourseSchedule(account.studentId, account.password, selectedXnm, selectedXqm)
    }

    fun fetchAndSaveCourseSchedule(
        username: String,
        pass: String,
        xnm: String = "2024",
        xqm: String = "3"
    ) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, statusMessage = "正在登录...")

                val result = withContext(Dispatchers.IO) {
                    val (loginSuccess, debugInfo) = try {
                        com.suseoaa.projectoaa.feature.course.SchoolSystem.login(username, pass)
                    } catch (e: Exception) {
                        false to e.message
                    }

                    if (!loginSuccess) return@withContext Triple(
                        null,
                        "登录失败: $debugInfo",
                        debugInfo
                    )

                    uiState =
                        uiState.copy(statusMessage = "正在获取 ${xnm}学年 ${if (xqm == "3") "上" else "下"}学期 课表...")

                    val (parsedData, scheduleDebugInfo) = com.suseoaa.projectoaa.feature.course.SchoolSystem.queryScheduleParsed(
                        xnm,
                        xqm
                    )
                    if (parsedData == null) return@withContext Triple(
                        null,
                        "解析失败: $scheduleDebugInfo",
                        scheduleDebugInfo
                    )

                    Triple(parsedData, null, "成功")
                }

                val (courseData, error, _) = result
                if (courseData != null) {
                    uiState = uiState.copy(statusMessage = "正在保存...")
                    withContext(Dispatchers.IO) {
                        repository.saveFromResponse(username, pass, courseData)
                    }
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "更新成功",
                        statusMessage = null
                    )

                    val currentId = _currentAccount.value?.studentId
                    val newAccount = savedAccounts.value.find { it.studentId == username }

                    if (newAccount != null) {
                        if (currentId != username) {
                            switchUser(newAccount, preserveSelection = false)
                        } else {
                            switchUser(newAccount, preserveSelection = true)
                        }
                    }

                } else {
                    uiState =
                        uiState.copy(isLoading = false, errorMessage = error, statusMessage = null)
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "异常: ${e.message}",
                    statusMessage = null
                )
            }
        }
    }

    fun addCustomCourse(
        name: String,
        location: String,
        teacher: String,
        weekday: Int,
        startNode: Int,
        duration: Int,
        weeks: String
    ) {
        val account = _currentAccount.value ?: return
        viewModelScope.launch {
            repository.saveCustomCourse(
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

    // 以前是给 UI 调用的，现在变成 ViewModel 内部的计算函数
    fun getCoursesForWeek(week: Int, allData: List<CourseWithTimes>): List<CourseWithTimes> {
        return calculateCoursesForWeek(week, allData)
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
}

data class CourseListUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)