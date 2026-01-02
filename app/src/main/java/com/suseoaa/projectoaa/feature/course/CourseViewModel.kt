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
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes
import com.suseoaa.projectoaa.core.data.repository.CourseRepository // 引用 Core 层的本地仓库
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

data class TermOption(
    val xnm: String, // 2024
    val xqm: String, // 3 或 12
    val label: String // "2024-2025 第1学期"
)

@HiltViewModel
class CourseListViewModel @Inject constructor(
    application: Application,
    // 注入新写的网络仓库
    private val schoolRepository: SchoolRepository
) : AndroidViewModel(application) {

    // 初始化本地数据库仓库 (保持原有逻辑，避免引入额外的 DI 模块复杂度)
    private val database = CourseDatabase.getInstance(application)
    private val localRepository = CourseRepository(database.courseDao())

    private val PREFS_NAME = "course_prefs"
    private val KEY_START_DATE = "semester_start_date"

    // === 状态管理 ===

    private val _currentAccount = MutableStateFlow<CourseAccountEntity?>(null)
    val currentAccount = _currentAccount.asStateFlow()

    // 监听本地数据库中的所有账号
    val savedAccounts: StateFlow<List<CourseAccountEntity>> = localRepository.allAccounts
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
    // 当账号、学年(xnm)、学期(xqm)变化时，自动从本地数据库重新查询
    @OptIn(ExperimentalCoroutinesApi::class)
    val allCourses: StateFlow<List<CourseWithTimes>> = combine(
        _currentAccount.filterNotNull(),
        snapshotFlow { selectedXnm },
        snapshotFlow { selectedXqm }
    ) { account, xnm, xqm ->
        Triple(account.studentId, xnm, xqm)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        localRepository.getCourses(studentId, xnm, xqm)
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 预计算所有周次的课表，优化 UI 性能
    val weekScheduleMap: StateFlow<Map<Int, List<CourseWithTimes>>> = allCourses
        .map { list ->
            (1..25).associateWith { week ->
                calculateCoursesForWeek(week, list)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())


    init {
        loadSemesterStart()
        // 自动选择第一个账号
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

        // 简单的学期推断逻辑：2-7月为下学期(12)，其他为上学期(3)
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
            localRepository.deleteAccount(account.studentId)
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
        // 切换学期后，可以考虑是否自动刷新，目前逻辑是手动刷新
    }

    fun refreshSchedule() {
        val account = _currentAccount.value
        if (account == null) {
            uiState = uiState.copy(errorMessage = "当前无账号")
            return
        }
        fetchAndSaveCourseSchedule(account.studentId, account.password, selectedXnm, selectedXqm)
    }

    /**
     * 核心功能：获取并保存课表
     * 1. 登录
     * 2. 获取课表数据
     * 3. 存入本地数据库
     * 4. 尝试获取校历并更新开学日期
     */
    fun fetchAndSaveCourseSchedule(
        username: String,
        pass: String,
        xnm: String = "2024",
        xqm: String = "3"
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, statusMessage = "正在登录...")

            // 1. 调用 SchoolRepository 登录
            val loginResult = schoolRepository.login(username, pass)

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "登录失败"
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = errorMsg,
                    statusMessage = null
                )
                return@launch
            }

            uiState =
                uiState.copy(statusMessage = "正在获取 ${xnm}学年 ${if (xqm == "3") "上" else "下"}学期 课表...")

            // 2. 登录成功，获取课表数据
            val courseResult = schoolRepository.getCourseSchedule(xnm, xqm)

            courseResult.onSuccess { courseData ->
                uiState = uiState.copy(statusMessage = "正在保存...")

                // 3. 保存到本地数据库 (IO 线程)
                withContext(Dispatchers.IO) {
                    localRepository.saveFromResponse(username, pass, courseData)
                }

                // 4. 尝试自动同步校历
                uiState = uiState.copy(statusMessage = "正在同步校历...")
                val startDateStr = schoolRepository.fetchSemesterStart()

                if (startDateStr != null) {
                    try {
                        val date = LocalDate.parse(startDateStr)
                        setSemesterStartDate(date)
                        println("自动设置起始周成功: $startDateStr")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    println("未能自动获取到起始周")
                }

                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "更新成功",
                    statusMessage = null
                )

                // 刷新当前用户选中状态
                val newAccount = savedAccounts.value.find { it.studentId == username }
                if (newAccount != null) {
                    val isSameUser = _currentAccount.value?.studentId == username
                    switchUser(newAccount, preserveSelection = isSameUser)
                }

            }.onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "获取课表失败: ${e.message}",
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