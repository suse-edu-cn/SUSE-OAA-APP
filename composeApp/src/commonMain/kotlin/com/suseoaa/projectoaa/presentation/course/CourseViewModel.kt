package com.suseoaa.projectoaa.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountInfo
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseUiState(
    val isLoading: Boolean = false,
    val courses: List<CourseWithTimes> = emptyList(),
    val currentWeek: Int = 1,
    val currentAccount: CourseAccountInfo? = null,
    val selectedXnm: String = "2024",
    val selectedXqm: String = "1",
    val errorMessage: String? = null
)

class CourseViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        // 计算当前周次
        calculateCurrentWeek()
    }

    private fun calculateCurrentWeek() {
        // 简单的周次计算，实际应该从服务器获取学期开始日期
        // 假设学期开始日期是 2024-02-26
        _uiState.update { it.copy(currentWeek = 1) }
    }

    fun setCurrentWeek(week: Int) {
        _uiState.update { it.copy(currentWeek = week) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
