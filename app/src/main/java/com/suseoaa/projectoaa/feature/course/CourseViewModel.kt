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
import com.suseoaa.projectoaa.core.data.repository.CourseRepository
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

data class TermOption(
    val xnm: String,
    val xqm: String,
    val label: String
)

@HiltViewModel
class CourseListViewModel @Inject constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val tokenManager: TokenManager
) : AndroidViewModel(application) {

    private val database = CourseDatabase.getInstance(application)
    private val localRepository = CourseRepository(database.courseDao())

    private val PREFS_NAME = "course_prefs"
    private val KEY_START_DATE = "semester_start_date"

    // 监听本地数据库中的所有账号 (按 sortIndex 排序)
    val savedAccounts: StateFlow<List<CourseAccountEntity>> = localRepository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 核心：CurrentAccount 响应式跟随 TokenManager 变化
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

    // 课程数据流
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

    init {
        loadSemesterStart()
        viewModelScope.launch {
            currentAccount.collect { account ->
                if (account != null) {
                    generateTermOptions(account.njdmId)
                    selectCurrentRealTerm()
                }
            }
        }
    }

    // 核心：切换用户时，只负责更新 DataStore
    fun switchUser(account: CourseAccountEntity) {
        viewModelScope.launch {
            tokenManager.saveCurrentStudentId(account.studentId)
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
            localRepository.deleteAccount(account.studentId)
        }
    }

    fun updateTermSelection(xnm: String, xqm: String) {
        selectedXnm = xnm
        selectedXqm = xqm
    }

    fun refreshSchedule() {
        val account = currentAccount.value
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
            uiState = uiState.copy(isLoading = true, statusMessage = "正在登录...")
            val loginResult = schoolRepository.login(username, pass)

            if (loginResult.isFailure) {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "登录失败"
                uiState =
                    uiState.copy(isLoading = false, errorMessage = errorMsg, statusMessage = null)
                return@launch
            }

            uiState = uiState.copy(statusMessage = "正在获取课表...")
            val courseResult = schoolRepository.getCourseSchedule(xnm, xqm)

            courseResult.onSuccess { courseData ->
                uiState = uiState.copy(statusMessage = "正在保存...")
                withContext(Dispatchers.IO) {
                    localRepository.saveFromResponse(username, pass, courseData)
                }

                uiState = uiState.copy(statusMessage = "正在同步校历...")
                val startDateStr = schoolRepository.fetchSemesterStart()
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
                    statusMessage = null
                )
                tokenManager.saveCurrentStudentId(username)

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
        name: String,
        location: String,
        teacher: String,
        weekday: Int,
        startNode: Int,
        duration: Int,
        weeks: String
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