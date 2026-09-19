package com.suseoaa.projectoaa.presentation.gpa

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.gpa.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.domain.repository.GpaRepository
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.domain.engine.scoreToGpaPoint
import com.suseoaa.projectoaa.domain.gpa.FilterType
import com.suseoaa.projectoaa.domain.gpa.GpaCalculator
import com.suseoaa.projectoaa.domain.gpa.SortOrder
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore

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
    val errorMessage: String? = null,
    /** 入学年份，用于把学期显示成「大一上」；取不到时学期回落到学年写法 */
    val enrollmentYear: Int? = null,
)

class GpaViewModel(
    private val sessionStore: SessionStore,
    private val userProfileStore: UserProfileStore,
    private val gpaRepository: GpaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpaUiState())
    val uiState: StateFlow<GpaUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 获取当前登录的学生ID
                val studentId = sessionStore.currentStudentId.first()
                val enrollmentYear = userProfileStore.userInfoFlow.first()["njdm_id"]
                    ?.take(4)?.toIntOrNull()
                _uiState.update { it.copy(enrollmentYear = enrollmentYear) }

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
                    val stats = GpaCalculator.stats(sortedCourses)

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

    /** 筛选、排序、统计三步都在 GpaCalculator 里，这里只负责把结果搬进 UiState。 */
    private fun applyFiltersAndSort(
        state: GpaUiState,
        allCourses: List<GpaCourseWrapper>,
        term: String,
        type: FilterType,
        order: SortOrder
    ): GpaUiState {
        val termFiltered = GpaCalculator.filterByTerm(allCourses, term)
        val stats = GpaCalculator.stats(termFiltered)
        val visible = GpaCalculator.sort(GpaCalculator.filterByType(termFiltered, type), order)

        return state.copy(
            courseList = visible,
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
                    val newGpa = scoreToGpaPoint(newScore)
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

}
