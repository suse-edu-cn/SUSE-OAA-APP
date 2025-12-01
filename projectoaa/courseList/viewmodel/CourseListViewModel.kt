package com.suseoaa.projectoaa.courseList.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.courseList.data.database.CourseDatabase
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.data.remote.SchoolSystem
import com.suseoaa.projectoaa.courseList.data.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CourseListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CourseDatabase.getInstance(application)
    private val repository = CourseRepository(database.courseDao())

    // === 账号管理 ===
    private val PREFS_ACCOUNT = "course_accounts"
    private val KEY_ACCOUNTS = "saved_accounts"
    private val KEY_CURRENT_ID = "current_student_id"

    private val _currentStudentId = MutableStateFlow(loadCurrentStudentId())
    val currentStudentId: StateFlow<String> = _currentStudentId.asStateFlow()

    private val _savedAccounts = MutableStateFlow<Map<String, String>>(loadAccounts())
    val savedAccounts: StateFlow<Map<String, String>> = _savedAccounts.asStateFlow()

    var uiState by mutableStateOf(CourseListUiState())
        private set

    // === 课程数据 ===
    val allCourses: StateFlow<List<CourseWithTimes>> = _currentStudentId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else repository.getCoursesByStudent(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === 周次管理 ===
    private val PREFS_NAME = "course_prefs"
    private val KEY_START_DATE = "semester_start_date"

    // 用户当前*查看*的周次（用于 UI 翻页）
    var currentDisplayWeek by mutableIntStateOf(1)

    // 基于时间计算的*真实*当前周次（用于标红显示）
    var realCurrentWeek by mutableIntStateOf(1)
        private set

    private var _semesterStartDate =
        MutableStateFlow<LocalDate>(LocalDate.now().with(DayOfWeek.MONDAY))
    val semesterStartDate: StateFlow<LocalDate> = _semesterStartDate

    init {
        loadSemesterStart()
    }

    // === 账号逻辑 (保持不变) ===
    private fun loadCurrentStudentId(): String {
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_ACCOUNT, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_ID, "") ?: ""
    }

    private fun loadAccounts(): Map<String, String> {
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_ACCOUNT, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ACCOUNTS, "[]")
        val map = mutableMapOf<String, String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                map[obj.getString("u")] = obj.getString("p")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun saveAccount(username: String, password: String) {
        val current = _savedAccounts.value.toMutableMap()
        current[username] = password
        _savedAccounts.value = current
        val jsonArr = JSONArray()
        current.forEach { (u, p) -> jsonArr.put(JSONObject().put("u", u).put("p", p)) }
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_ACCOUNT, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCOUNTS, jsonArr.toString()).apply()
    }

    fun switchUser(studentId: String) {
        _currentStudentId.value = studentId
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_ACCOUNT, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_ID, studentId).apply()
    }

    /**
     * 刷新当前显示的账号课表
     * 使用本地保存的密码进行更新
     */
    fun refreshSchedule() {
        val currentId = _currentStudentId.value
        if (currentId.isBlank()) {
            uiState = uiState.copy(errorMessage = "当前未选择账号，无法刷新")
            return
        }

        // 从保存的列表中查找当前账号的密码
        val password = _savedAccounts.value[currentId]
        if (password.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "未找到账号 $currentId 的保存密码，请尝试重新导入")
            return
        }

        // 复用 fetchAndSaveCourseSchedule 进行更新
        fetchAndSaveCourseSchedule(currentId, password)
    }

    fun fetchAndSaveCourseSchedule(username: String, password: String) {
        viewModelScope.launch {
            try {
                saveAccount(username, password)
                switchUser(username)
                uiState = uiState.copy(isLoading = true, statusMessage = "正在登录...")
                val result = withContext(Dispatchers.IO) {
                    val (loginSuccess, debugInfo) = SchoolSystem.login(username, password)
                    if (!loginSuccess) return@withContext Triple(
                        null,
                        "登录失败: $debugInfo",
                        debugInfo
                    )
                    uiState = uiState.copy(statusMessage = "正在获取课表...")
                    val (parsedData, scheduleDebugInfo) = SchoolSystem.queryScheduleParsed()
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
                        repository.saveFromResponse(
                            username,
                            courseData
                        )
                    }
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "更新成功",
                        statusMessage = null
                    )
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

    // === 周次逻辑更新 ===
    private fun loadSemesterStart() {
        val prefs =
            getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val epochDay = prefs.getLong(KEY_START_DATE, -1L)
        val today = LocalDate.now()

        // 默认为本周一
        val start =
            if (epochDay != -1L) LocalDate.ofEpochDay(epochDay) else today.with(DayOfWeek.MONDAY)
        _semesterStartDate.value = start

        // 计算真实周次
        val weeksBetween = ChronoUnit.WEEKS.between(start, today).toInt() + 1
        realCurrentWeek = weeksBetween

        // 默认显示真实周次 (限制在1-25之间)
        currentDisplayWeek = weeksBetween.coerceIn(1, 25)
    }

    fun setSemesterStartDate(date: LocalDate) {
        val monday = date.with(DayOfWeek.MONDAY)
        viewModelScope.launch {
            val prefs =
                getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_START_DATE, monday.toEpochDay()).apply()
            _semesterStartDate.value = monday

            val today = LocalDate.now()
            val weeksBetween = ChronoUnit.WEEKS.between(monday, today).toInt() + 1
            realCurrentWeek = weeksBetween
            currentDisplayWeek = weeksBetween.coerceIn(1, 25)
        }
    }

    fun getCoursesForWeek(week: Int, allData: List<CourseWithTimes>): List<CourseWithTimes> {
        if (allData.isEmpty()) return emptyList()
        return allData.mapNotNull { courseWithTimes ->
            val validTimes = courseWithTimes.times.filter { time ->
                val weekBit = 1L shl (week - 1)
                (time.weeksMask and weekBit) != 0L
            }
            if (validTimes.isNotEmpty()) courseWithTimes.copy(times = validTimes) else null
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(successMessage = null, errorMessage = null, statusMessage = null)
    }
}

data class CourseListUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)