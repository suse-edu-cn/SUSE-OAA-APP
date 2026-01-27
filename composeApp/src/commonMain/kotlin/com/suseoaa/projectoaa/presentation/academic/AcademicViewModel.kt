package com.suseoaa.projectoaa.presentation.academic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AcademicRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcademicUiState(
    val isLoading: Boolean = false,
    val messages: List<String> = emptyList(),
    val exams: List<ExamItem> = emptyList(),
    val errorMessage: String? = null
)

class AcademicViewModel(
    private val academicRepository: AcademicRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AcademicUiState())
    val uiState: StateFlow<AcademicUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 加载考试信息
            when (val result = academicRepository.getExams()) {
                is Result.Success -> {
                    _uiState.update { it.copy(exams = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
            
            // 加载教务消息
            when (val result = academicRepository.getAcademicMessages()) {
                is Result.Success -> {
                    val messageTexts = result.data.mapNotNull { it.bt }
                    _uiState.update { it.copy(messages = messageTexts) }
                }
                is Result.Error -> {
                    // 静默处理消息错误
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
