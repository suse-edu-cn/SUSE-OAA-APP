package com.suseoaa.projectoaa.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountInfo
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.domain.model.course.TermOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class CourseUiState(
    val isLoading: Boolean = false,
    val courses: List<CourseWithTimes> = emptyList(),
    val currentWeek: Int = 1,
    val realCurrentWeek: Int = 1,
    val currentAccount: CourseAccountInfo? = null,
    val accountName: String? = null,
    val className: String? = null,
    val selectedXnm: String = "2024",
    val selectedXqm: String = "1",
    val currentTermLabel: String? = null,
    val termOptions: List<TermOption> = emptyList(),
    val semesterStartDate: LocalDate? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CourseViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        // 初始化学期选项
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val termOptions = listOf(
            TermOption("${currentYear - 1}-${currentYear}学年 第一学期", "${currentYear - 1}", "1"),
            TermOption("${currentYear - 1}-${currentYear}学年 第二学期", "${currentYear - 1}", "2"),
            TermOption("${currentYear}-${currentYear + 1}学年 第一学期", "$currentYear", "1"),
            TermOption("${currentYear}-${currentYear + 1}学年 第二学期", "$currentYear", "2"),
        )
        
        // 默认学期开始日期（示例：2024年2月26日）
        val defaultStartDate = LocalDate(currentYear, 2, 26)
        
        // 计算当前真实周次
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysSinceStart = today.toEpochDays() - defaultStartDate.toEpochDays()
        val realCurrentWeek = if (daysSinceStart >= 0) {
            (daysSinceStart / 7 + 1).toInt().coerceIn(1, 25)
        } else {
            1
        }
        
        _uiState.update { 
            it.copy(
                termOptions = termOptions,
                currentTermLabel = termOptions.getOrNull(2)?.label,
                selectedXnm = "$currentYear",
                selectedXqm = "1",
                semesterStartDate = defaultStartDate,
                currentWeek = realCurrentWeek,
                realCurrentWeek = realCurrentWeek
            )
        }
    }

    fun setCurrentWeek(week: Int) {
        _uiState.update { it.copy(currentWeek = week) }
    }

    fun updateTermSelection(xnm: String, xqm: String) {
        _uiState.update { state ->
            val label = state.termOptions.find { it.xnm == xnm && it.xqm == xqm }?.label
            state.copy(
                selectedXnm = xnm,
                selectedXqm = xqm,
                currentTermLabel = label
            )
        }
        // 加载对应学期的课表
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // TODO: 从仓库加载课程数据
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun refreshSchedule() {
        loadCourses()
    }

    fun fetchAndSaveCourseSchedule(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // TODO: 调用 API 获取并保存课表
                // 模拟成功
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "课表导入成功",
                        accountName = username
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "导入失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSemesterStartDate(date: LocalDate) {
        _uiState.update { state ->
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val daysSinceStart = today.toEpochDays() - date.toEpochDays()
            val realCurrentWeek = if (daysSinceStart >= 0) {
                (daysSinceStart / 7 + 1).toInt().coerceIn(1, 25)
            } else {
                1
            }
            state.copy(
                semesterStartDate = date,
                realCurrentWeek = realCurrentWeek,
                currentWeek = realCurrentWeek
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
