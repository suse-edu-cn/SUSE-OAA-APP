package com.suseoaa.projectoaa.presentation.gpa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AcademicRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.domain.model.grade.FilterType
import com.suseoaa.projectoaa.shared.domain.model.grade.GpaCourseWrapper
import com.suseoaa.projectoaa.shared.domain.model.grade.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val academicRepository: AcademicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpaUiState())
    val uiState: StateFlow<GpaUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = academicRepository.getGrades()) {
                is Result.Success -> {
                    val grades = result.data
                    val wrappers = academicRepository.toGpaCourseWrappers(grades)
                    val stats = academicRepository.calculateGpaStats(wrappers)
                    
                    val filtered = academicRepository.filterAndSortCourses(
                        wrappers,
                        _uiState.value.filterType,
                        _uiState.value.sortOrder
                    )
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allCourses = wrappers,
                            courseList = filtered,
                            totalGpa = stats.totalGpa,
                            totalCredits = stats.totalCredits,
                            degreeGpa = stats.degreeGpa,
                            degreeCredits = stats.degreeCredits
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val filtered = academicRepository.filterAndSortCourses(
                state.allCourses,
                state.filterType,
                order
            )
            state.copy(sortOrder = order, courseList = filtered)
        }
    }

    fun setFilterType(type: FilterType) {
        _uiState.update { state ->
            val filtered = academicRepository.filterAndSortCourses(
                state.allCourses,
                type,
                state.sortOrder
            )
            state.copy(filterType = type, courseList = filtered)
        }
    }

    fun updateSimulatedScore(item: GpaCourseWrapper, newScore: Double) {
        _uiState.update { state ->
            val updatedAllCourses = state.allCourses.map { course ->
                if (course.originalEntity == item.originalEntity) {
                    course.copy(simulatedScore = newScore)
                } else {
                    course
                }
            }
            
            val stats = academicRepository.calculateGpaStats(updatedAllCourses)
            val filtered = academicRepository.filterAndSortCourses(
                updatedAllCourses,
                state.filterType,
                state.sortOrder
            )
            
            state.copy(
                allCourses = updatedAllCourses,
                courseList = filtered,
                totalGpa = stats.totalGpa,
                totalCredits = stats.totalCredits,
                degreeGpa = stats.degreeGpa,
                degreeCredits = stats.degreeCredits
            )
        }
    }
}
