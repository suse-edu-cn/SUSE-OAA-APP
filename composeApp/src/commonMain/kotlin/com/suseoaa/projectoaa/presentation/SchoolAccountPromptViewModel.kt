package com.suseoaa.projectoaa.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAccountRepository
import com.suseoaa.projectoaa.util.SchoolAccountPromptManager
import com.suseoaa.projectoaa.util.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SchoolAccountPromptUiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val hasDefaultAccount: Boolean = false
)

/**
 * 全局「添加教务系统账号」弹窗 ViewModel。
 */
class SchoolAccountPromptViewModel(
    private val schoolAccountRepository: SchoolAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchoolAccountPromptUiState())
    val uiState: StateFlow<SchoolAccountPromptUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            schoolAccountRepository.observeHasDefaultAccount().collect { hasDefault ->
                _uiState.update { it.copy(hasDefaultAccount = hasDefault) }
            }
        }
    }

    fun resetFormState() {
        _uiState.update {
            it.copy(
                isLoading = false,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun addAccount(studentId: String, password: String) {
        if (_uiState.value.isLoading) return
        val trimmedId = studentId.trim()
        if (trimmedId.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入学号和教务系统密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = "正在登录教务系统并同步账号信息...",
                    errorMessage = null
                )
            }

            schoolAccountRepository.addSchoolAccount(
                studentId = trimmedId,
                password = password,
                forceSetDefault = false
            ).onSuccess { added ->
                val msg = if (added.isSetAsDefault) {
                    "教务账号添加成功，已设为默认账号"
                } else {
                    "教务账号添加成功"
                }
                ToastManager.showSuccess(msg)
                resetFormState()
                SchoolAccountPromptManager.complete()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "教务系统登录失败，请检查学号和密码"
                    )
                }
            }
        }
    }
}
