package com.suseoaa.projectoaa.presentation.gpa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.GpaCourseWrapper
import com.suseoaa.projectoaa.data.repository.GpaRepository
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// KMP 兼容的格式化函数（四舍五入）
private fun Double.format(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
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

enum class SortOrder {
    DESCENDING, // 从高到低
    ASCENDING   // 从低到高
}

enum class FilterType {
    ALL,        // 全部课程
    DEGREE_ONLY // 仅学位课
}

data class GpaUiState(
    val isLoading: Boolean = false,
    val courseList: List<GpaCourseWrapper> = emptyList(),
    val allCourses: List<GpaCourseWrapper> = emptyList(),
    val totalGpa: String = "0.00",
    val totalCredits: String = "0.0",
    val degreeGpa: String = "0.00",
    val degreeCredits: String = "0.0",
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val filterType: FilterType = FilterType.ALL,
    val errorMessage: String? = null
)

class GpaViewModel(
    private val tokenManager: TokenManager,
    private val gpaRepository: GpaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpaUiState())
    val uiState: StateFlow<GpaUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // 获取当前登录的学生ID
                val studentId = tokenManager.currentStudentId.first()
                
                if (studentId.isNullOrEmpty()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "请先登录"
                        )
                    }
                    return@launch
                }
                
                // 从 Repository 获取 GPA 数据
                val result = gpaRepository.getGpaData(studentId)
                
                result.onSuccess { courses ->
                    val sortedCourses = courses.sortedByDescending { it.scoreValue }
                    val stats = calculateTotalStats(sortedCourses)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            allCourses = sortedCourses,
                            courseList = sortedCourses,
                            totalGpa = stats.totalGpa,
                            totalCredits = stats.totalCredits,
                            degreeGpa = stats.degreeGpa,
                            degreeCredits = stats.degreeCredits,
                            errorMessage = null
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "加载失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载成绩失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val sorted = when (order) {
                SortOrder.DESCENDING -> state.courseList.sortedByDescending { it.scoreValue }
                SortOrder.ASCENDING -> state.courseList.sortedBy { it.scoreValue }
            }
            state.copy(sortOrder = order, courseList = sorted)
        }
    }

    fun setFilterType(type: FilterType) {
        _uiState.update { state ->
            val filtered = when (type) {
                FilterType.ALL -> state.allCourses
                FilterType.DEGREE_ONLY -> state.allCourses.filter { it.isDegreeCourse }
            }
            // 应用当前排序
            val sorted = when (state.sortOrder) {
                SortOrder.DESCENDING -> filtered.sortedByDescending { it.scoreValue }
                SortOrder.ASCENDING -> filtered.sortedBy { it.scoreValue }
            }
            // 重新计算统计数据
            val stats = calculateTotalStats(filtered)
            state.copy(
                filterType = type, 
                courseList = sorted,
                totalGpa = if (type == FilterType.DEGREE_ONLY) stats.degreeGpa else stats.totalGpa,
                totalCredits = if (type == FilterType.DEGREE_ONLY) stats.degreeCredits else stats.totalCredits
            )
        }
    }

    fun updateSimulatedScore(item: GpaCourseWrapper, newScore: Double) {
        _uiState.update { state ->
            val updatedAllCourses = state.allCourses.map { course ->
                if (course.originalEntity.courseId == item.originalEntity.courseId) {
                    // 计算新的绩点
                    val newGpa = calculateSingleGpa(newScore)
                    course.copy(simulatedScore = newScore, simulatedGpa = newGpa)
                } else {
                    course
                }
            }
            
            val stats = calculateTotalStats(updatedAllCourses)
            val filtered = when (state.filterType) {
                FilterType.ALL -> updatedAllCourses
                FilterType.DEGREE_ONLY -> updatedAllCourses.filter { it.isDegreeCourse }
            }
            
            val sorted = when (state.sortOrder) {
                SortOrder.DESCENDING -> filtered.sortedByDescending { it.scoreValue }
                SortOrder.ASCENDING -> filtered.sortedBy { it.scoreValue }
            }
            
            state.copy(
                allCourses = updatedAllCourses,
                courseList = sorted,
                totalGpa = stats.totalGpa,
                totalCredits = stats.totalCredits,
                degreeGpa = stats.degreeGpa,
                degreeCredits = stats.degreeCredits
            )
        }
    }
    
    /**
     * 计算单科绩点
     * 与 Android 版本保持一致
     */
    private fun calculateSingleGpa(score: Double): Double {
        return when {
            score >= 95.0 -> 4.5
            score < 60.0 -> 0.0
            else -> {
                val base = 1.0
                val steps = ((score - 60) / 5).toInt()
                base + steps * 0.5
            }
        }
    }
    
    private data class GpaStats(
        val totalGpa: String,
        val totalCredits: String,
        val degreeGpa: String,
        val degreeCredits: String
    )
    
    private fun calculateTotalStats(courses: List<GpaCourseWrapper>): GpaStats {
        var totalPoints = 0.0
        var totalCredits = 0.0
        var degreePoints = 0.0
        var degreeCredits = 0.0
        
        courses.forEach { item ->
            val credit = item.credit
            if (credit > 0.0) {
                // 所有课程都参与绩点计算
                totalPoints += item.gpaValue * credit
                totalCredits += credit
                
                if (item.isDegreeCourse) {
                    degreePoints += item.gpaValue * credit
                    degreeCredits += credit
                }
            }
        }
        
        val finalTotalGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val finalDegreeGpa = if (degreeCredits > 0) degreePoints / degreeCredits else 0.0
        
        return GpaStats(
            totalGpa = finalTotalGpa.format(2),
            totalCredits = totalCredits.format(1),
            degreeGpa = finalDegreeGpa.format(2),
            degreeCredits = degreeCredits.format(1)
        )
    }
}
