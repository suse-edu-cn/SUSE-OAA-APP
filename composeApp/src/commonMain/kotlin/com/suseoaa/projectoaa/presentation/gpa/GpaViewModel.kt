package com.suseoaa.projectoaa.presentation.gpa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.presentation.grades.GradeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// KMP 兼容的格式化函数
private fun Double.format(decimals: Int): String {
    // 简单实现：转换为字符串然后截取
    val str = this.toString()
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

/**
 * 绩点课程包装类
 */
data class GpaCourseWrapper(
    val originalEntity: GradeItem,
    val simulatedScore: Double? = null,
    val isDegreeCourse: Boolean = false
) {
    val displayScore: String
        get() = simulatedScore?.toString() ?: originalEntity.cj ?: "0"
    
    val displayGpa: String
        get() {
            val score = simulatedScore ?: (originalEntity.cj?.toDoubleOrNull() ?: 0.0)
            return calculateGpa(score)
        }
    
    val credit: Double
        get() = originalEntity.xf?.toDoubleOrNull() ?: 0.0
    
    val simulatedGpa: Double
        get() {
            val score = simulatedScore ?: (originalEntity.cj?.toDoubleOrNull() ?: 0.0)
            return calculateGpaValue(score)
        }
    
    private fun calculateGpa(score: Double): String {
        return calculateGpaValue(score).format(2)
    }
    
    private fun calculateGpaValue(score: Double): Double {
        return when {
            score >= 90 -> 4.0
            score >= 85 -> 3.7
            score >= 82 -> 3.3
            score >= 78 -> 3.0
            score >= 75 -> 2.7
            score >= 72 -> 2.3
            score >= 68 -> 2.0
            score >= 64 -> 1.5
            score >= 60 -> 1.0
            else -> 0.0
        }
    }
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
    private val localCourseRepository: LocalCourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpaUiState())
    val uiState: StateFlow<GpaUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // TODO: 从 GradeRepository 加载成绩数据并计算绩点
            // 需要实现 SchoolGradeRepository 来获取成绩信息
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "绩点计算功能正在开发中，请先在成绩查询页面同步成绩"
                )
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val sorted = when (order) {
                SortOrder.DESCENDING -> state.courseList.sortedByDescending { it.simulatedScore ?: it.originalEntity.cj?.toDoubleOrNull() ?: 0.0 }
                SortOrder.ASCENDING -> state.courseList.sortedBy { it.simulatedScore ?: it.originalEntity.cj?.toDoubleOrNull() ?: 0.0 }
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
            state.copy(filterType = type, courseList = filtered)
        }
    }

    fun updateSimulatedScore(item: GpaCourseWrapper, newScore: Double) {
        _uiState.update { state ->
            val updatedAllCourses = state.allCourses.map { course ->
                if (course.originalEntity.kch == item.originalEntity.kch) {
                    course.copy(simulatedScore = newScore)
                } else {
                    course
                }
            }
            
            val stats = calculateTotalStats(updatedAllCourses)
            val filtered = when (state.filterType) {
                FilterType.ALL -> updatedAllCourses
                FilterType.DEGREE_ONLY -> updatedAllCourses.filter { it.isDegreeCourse }
            }
            
            state.copy(
                allCourses = updatedAllCourses,
                courseList = filtered,
                totalGpa = stats[0],
                totalCredits = stats[1],
                degreeGpa = stats[2],
                degreeCredits = stats[3]
            )
        }
    }
    
    private fun calculateTotalStats(courses: List<GpaCourseWrapper>): List<String> {
        var totalPoints = 0.0
        var totalCredits = 0.0
        var degreePoints = 0.0
        var degreeCredits = 0.0
        
        courses.forEach { item ->
            val credit = item.credit
            if (credit > 0.0) {
                totalPoints += item.simulatedGpa * credit
                totalCredits += credit
                
                if (item.isDegreeCourse) {
                    degreePoints += item.simulatedGpa * credit
                    degreeCredits += credit
                }
            }
        }
        
        val finalTotalGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val finalDegreeGpa = if (degreeCredits > 0) degreePoints / degreeCredits else 0.0
        
        return listOf(
            finalTotalGpa.format(2),
            totalCredits.format(1),
            finalDegreeGpa.format(2),
            degreeCredits.format(1)
        )
    }
}
