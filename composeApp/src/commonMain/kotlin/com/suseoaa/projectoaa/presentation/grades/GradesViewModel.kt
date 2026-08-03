package com.suseoaa.projectoaa.presentation.grades

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.data.repository.GradeEntity
import com.suseoaa.projectoaa.shared.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.data.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.shared.domain.model.grade.StudentGradeResponse
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeItem
import com.suseoaa.projectoaa.shared.data.repository.GpaRepository
import kotlin.math.pow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Immutable
data class GradesUiState(
    val isRefreshing: Boolean = false,
    val grades: List<GradeEntity> = emptyList(),
    val selectedYear: String = "",
    val selectedSemester: String = "3",
    val startYear: Int = 2020,
    val message: String? = null,
    val currentAccount: CourseAccountEntity? = null,
    val totalGpa: String = "0.00",
    val totalDegreeGpa: String = "0.00",
    val currentTermGpa: String = "0.00",
    val currentTermDegreeGpa: String = "0.00"
)

class GradesViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolGradeRepository: SchoolGradeRepository,
    private val gpaRepository: GpaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState())
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    // 当前账户流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount: StateFlow<CourseAccountEntity?> = combine(
        localCourseRepository.getAllAccounts(),
        tokenManager.currentStudentId
    ) { accounts, selectedId ->
        if (accounts.isEmpty()) null
        else accounts.find { it.studentId == selectedId } ?: accounts.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    // 成绩数据流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val gradesFlow: StateFlow<List<GradeEntity>> = combine(
        currentAccount.filterNotNull(),
        _uiState
    ) { account, state ->
        Triple(account.studentId, state.selectedYear, state.selectedSemester)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        schoolGradeRepository.observeGrades(studentId, xnm, xqm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    init {
        // 初始化当前学年
        val currentYear = com.suseoaa.projectoaa.shared.util.OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val currentMonth =
            com.suseoaa.projectoaa.shared.util.OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
        // 如果在上半年(1-7月)，使用上一年作为学年
        val academicYear = if (currentMonth < 8) currentYear - 1 else currentYear

        _uiState.update {
            it.copy(
                selectedYear = academicYear.toString(),
                startYear = academicYear - 4
            )
        }

        // 观察当前账户变化
        viewModelScope.launch {
            currentAccount.collect { account ->
                _uiState.update { it.copy(currentAccount = account) }
                // 更新起始年份
                account?.let { acc ->
                    val startYear = acc.njdmId.toIntOrNull() ?: (academicYear - 4)
                    _uiState.update { it.copy(startYear = startYear) }
                }
            }
        }

        // 观察成绩数据变化
        viewModelScope.launch {
            gradesFlow.collect { grades ->
                _uiState.update { it.copy(grades = grades) }
                // 当成绩变化时，重新计算绩点统计
                currentAccount.value?.let { account ->
                    calculateGpaStats(account.studentId)
                }
            }
        }
    }

    private fun calculateGpaStats(studentId: String) {
        viewModelScope.launch {
            val result = gpaRepository.getGpaData(studentId)
            result.onSuccess { allCourses ->
                var totalPoints = 0.0
                var totalCredits = 0.0
                var degreePoints = 0.0
                var degreeCredits = 0.0

                var currentPoints = 0.0
                var currentCredits = 0.0
                var currentDegreePoints = 0.0
                var currentDegreeCredits = 0.0

                val currentState = _uiState.value
                val selectedYear = currentState.selectedYear
                val selectedSemester = currentState.selectedSemester

                allCourses.forEach { item ->
                    if (!item.isIncludedInCalculation) return@forEach
                    
                    val credit = item.credit
                    // 排除没有成绩或者模拟成绩为空的项
                    if (credit > 0.0 && item.originalScoreText.isNotEmpty()) {
                        val gpa = item.gpaValue
                        
                        totalPoints += gpa * credit
                        totalCredits += credit
                        
                        if (item.isDegreeCourse) {
                            degreePoints += gpa * credit
                            degreeCredits += credit
                        }
                        
                        // Check if it belongs to current term
                        if (item.originalEntity.xnm == selectedYear && item.originalEntity.xqm == selectedSemester) {
                            currentPoints += gpa * credit
                            currentCredits += credit
                            
                            if (item.isDegreeCourse) {
                                currentDegreePoints += gpa * credit
                                currentDegreeCredits += credit
                            }
                        }
                    }
                }
                
                fun Double.format(decimals: Int): String {
                    val factor = 10.0.pow(decimals)
                    val rounded = kotlin.math.round(this * factor) / factor
                    val str = rounded.toString()
                    val parts = str.split(".")
                    return if (parts.size == 1) {
                        "$str.${"0".repeat(decimals)}"
                    } else {
                        val intPart = parts[0]
                        val decimalPart = parts[1]
                        if (decimalPart.length >= decimals) {
                            "$intPart.${decimalPart.take(decimals)}"
                        } else {
                            "$intPart.$decimalPart${"0".repeat(decimals - decimalPart.length)}"
                        }
                    }
                }

                val finalTotalGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
                val finalDegreeGpa = if (degreeCredits > 0) degreePoints / degreeCredits else 0.0
                val finalCurrentGpa = if (currentCredits > 0) currentPoints / currentCredits else 0.0
                val finalCurrentDegreeGpa = if (currentDegreeCredits > 0) currentDegreePoints / currentDegreeCredits else 0.0

                _uiState.update { 
                    it.copy(
                        totalGpa = finalTotalGpa.format(2),
                        totalDegreeGpa = finalDegreeGpa.format(2),
                        currentTermGpa = finalCurrentGpa.format(2),
                        currentTermDegreeGpa = finalCurrentDegreeGpa.format(2)
                    )
                }
            }
        }
    }

    fun refreshGrades() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true, message = "正在连接教务系统...") }

            try {
                // 自动重试登录，确保 Session 有效
                val loginResult = schoolAuthRepository.login(account.studentId, account.password)

                if (loginResult.isSuccess) {
                    _uiState.update { it.copy(message = "正在全量同步成绩...") }
                    val result = schoolGradeRepository.fetchAllHistoryGrades(account)

                    result.onSuccess { msg ->
                        _uiState.update { it.copy(message = msg) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(message = "更新失败: ${e.message}") }
                    }
                } else {
                    _uiState.update { it.copy(message = "教务登录失败，请检查密码或网络") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "未知错误: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshCurrentTermGrades() {
        val account = currentAccount.value ?: return
        val state = _uiState.value
        viewModelScope.launch {
            if (state.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true, message = "正在连接教务系统...") }

            try {
                // 自动重试登录，确保 Session 有效
                val loginResult = schoolAuthRepository.login(account.studentId, account.password)

                if (loginResult.isSuccess) {
                    _uiState.update { it.copy(message = "正在同步当前学期成绩...") }
                    val result = schoolGradeRepository.fetchCurrentTermGrades(
                        account,
                        state.selectedYear,
                        state.selectedSemester
                    )

                    result.onSuccess { msg ->
                        _uiState.update { it.copy(message = msg) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(message = "更新失败: ${e.message}") }
                    }
                } else {
                    _uiState.update { it.copy(message = "教务登录失败，请检查密码或网络") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "未知错误: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun updateFilter(year: String, semester: String) {
        _uiState.update { state ->
            state.copy(
                selectedYear = year,
                selectedSemester = semester
            )
        }
        currentAccount.value?.let { account ->
            calculateGpaStats(account.studentId)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
