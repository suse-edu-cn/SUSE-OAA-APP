package com.suseoaa.projectoaa.presentation.gpa

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.data.repository.GpaRepository
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

@Immutable
data class GpaUiState(
    val isLoading: Boolean = false,
    val courseList: List<GpaCourseWrapper> = emptyList(),
    val allCourses: List<GpaCourseWrapper> = emptyList(),
    val termList: List<String> = emptyList(),
    val selectedTerm: String = "ALL",
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
                    val terms = courses.map { "${it.originalEntity.xnm}_${it.originalEntity.xqm}" }
                        .distinct()
                        .sortedByDescending {
                            val parts = it.split("_")
                            val year = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val term = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            year * 100 + term
                        }
                    val stats = calculateTotalStats(sortedCourses)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allCourses = sortedCourses,
                            courseList = sortedCourses,
                            termList = terms,
                            selectedTerm = "ALL",
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

    private fun applyFiltersAndSort(
        state: GpaUiState,
        allCourses: List<GpaCourseWrapper>,
        term: String,
        type: FilterType,
        order: SortOrder
    ): GpaUiState {
        val termFiltered = if (term == "ALL") allCourses
        else allCourses.filter { "${it.originalEntity.xnm}_${it.originalEntity.xqm}" == term }

        val stats = calculateTotalStats(termFiltered)

        val typeFiltered = if (type == FilterType.DEGREE_ONLY) termFiltered.filter { it.isDegreeCourse } else termFiltered

        val sorted = when (order) {
            SortOrder.DESCENDING -> typeFiltered.sortedByDescending { it.scoreValue }
            SortOrder.ASCENDING -> typeFiltered.sortedBy { it.scoreValue }
        }

        return state.copy(
            courseList = sorted,
            selectedTerm = term,
            filterType = type,
            sortOrder = order,
            totalGpa = stats.totalGpa,
            totalCredits = stats.totalCredits,
            degreeGpa = stats.degreeGpa,
            degreeCredits = stats.degreeCredits
        )
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state -> applyFiltersAndSort(state, state.allCourses, state.selectedTerm, state.filterType, order) }
    }

    fun setFilterType(type: FilterType) {
        _uiState.update { state -> applyFiltersAndSort(state, state.allCourses, state.selectedTerm, type, state.sortOrder) }
    }

    fun setTermFilter(term: String) {
        _uiState.update { state -> applyFiltersAndSort(state, state.allCourses, term, state.filterType, state.sortOrder) }
    }

    fun updateSimulatedScoreByCourseId(courseId: String, newScore: Double) {
        _uiState.update { state ->
            val updatedAllCourses = state.allCourses.map { course ->
                if (course.originalEntity.courseId == courseId || course.originalEntity.courseName == courseId) {
                    val newGpa = calculateSingleGpa(newScore)
                    course.copy(simulatedScore = newScore, simulatedGpa = newGpa)
                } else {
                    course
                }
            }
            val newState = state.copy(allCourses = updatedAllCourses)
            applyFiltersAndSort(newState, updatedAllCourses, state.selectedTerm, state.filterType, state.sortOrder)
        }
    }

    fun updateCourseInclusion(courseId: String, isIncluded: Boolean) {
        _uiState.update { state ->
            val updatedAllCourses = state.allCourses.map { course ->
                if (course.originalEntity.courseId == courseId || course.originalEntity.courseName == courseId) {
                    course.copy(isIncludedInCalculation = isIncluded)
                } else {
                    course
                }
            }
            val newState = state.copy(allCourses = updatedAllCourses)
            applyFiltersAndSort(newState, updatedAllCourses, state.selectedTerm, state.filterType, state.sortOrder)
        }
    }

    fun updateSimulatedScore(item: GpaCourseWrapper, newScore: Double) {
        updateSimulatedScoreByCourseId(item.originalEntity.courseId.ifEmpty { item.originalEntity.courseName }, newScore)
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
            if (!item.isIncludedInCalculation) return@forEach
            
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
