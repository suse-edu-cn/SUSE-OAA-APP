package com.suseoaa.projectoaa.presentation.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 修改密码界面状态
 */
data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * 修改密码 ViewModel
 */
class ChangePasswordViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun updateOldPassword(password: String) {
        _uiState.update { it.copy(oldPassword = password, errorMessage = null) }
    }

    fun updateNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun changePassword() {
        val currentState = _uiState.value
        
        // 验证
        when {
            currentState.oldPassword.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "请输入原密码") }
                return
            }
            currentState.newPassword.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "请输入新密码") }
                return
            }
            currentState.newPassword.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "新密码长度至少6位") }
                return
            }
            currentState.newPassword != currentState.confirmPassword -> {
                _uiState.update { it.copy(errorMessage = "两次密码输入不一致") }
                return
            }
            currentState.oldPassword == currentState.newPassword -> {
                _uiState.update { it.copy(errorMessage = "新密码不能与原密码相同") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = userRepository.changePassword(
                currentState.oldPassword,
                currentState.newPassword,
                currentState.confirmPassword
            )) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            successMessage = "密码修改成功"
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
                is Result.Loading -> {
                    // 已处理
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
