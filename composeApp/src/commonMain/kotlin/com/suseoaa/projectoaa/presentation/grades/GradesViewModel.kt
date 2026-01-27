package com.suseoaa.projectoaa.presentation.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AcademicRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.domain.model.grade.GradeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class GradesUiState(
    val isRefreshing: Boolean = false,
    val grades: List<GradeItem> = emptyList(),
    val allGrades: List<GradeItem> = emptyList(),
    val selectedYear: String = "",
    val selectedSemester: String = "3",
    val startYear: Int = 2020,
    val message: String? = null
)

class GradesViewModel(
    private val academicRepository: AcademicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState())
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    init {
        // 初始化当前学年
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
        // 如果在上半年(1-7月)，使用上一年作为学年
        val academicYear = if (currentMonth < 8) currentYear - 1 else currentYear
        
        _uiState.update {
            it.copy(
                selectedYear = academicYear.toString(),
                startYear = academicYear - 4
            )
        }
        
        loadGrades()
    }

    fun loadGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            when (val result = academicRepository.getGrades()) {
                is Result.Success -> {
                    val allGrades = result.data
                    val filtered = filterGrades(allGrades, _uiState.value.selectedYear, _uiState.value.selectedSemester)
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            allGrades = allGrades,
                            grades = filtered
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = result.message
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    fun refreshGrades() {
        loadGrades()
    }

    fun updateFilter(year: String, semester: String) {
        _uiState.update { state ->
            val filtered = filterGrades(state.allGrades, year, semester)
            state.copy(
                selectedYear = year,
                selectedSemester = semester,
                grades = filtered
            )
        }
    }

    private fun filterGrades(grades: List<GradeItem>, year: String, semester: String): List<GradeItem> {
        return grades.filter { grade ->
            val yearMatch = grade.xnm == year || grade.xnmmc?.contains(year) == true
            val semesterMatch = grade.xqm == semester
            yearMatch && semesterMatch
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
