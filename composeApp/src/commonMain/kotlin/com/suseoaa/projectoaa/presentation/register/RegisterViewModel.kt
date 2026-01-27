package com.suseoaa.projectoaa.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AuthRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 注册界面状态
 */
data class RegisterUiState(
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccess: Boolean = false
)

/**
 * 注册 ViewModel
 */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateAccount(account: String) {
        if (account.length <= 20) {
            _uiState.update { it.copy(account = account, errorMessage = null) }
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun register() {
        val currentState = _uiState.value
        val cleanAccount = currentState.account.trim()
        val cleanPassword = currentState.password.trim()
        val cleanConfirmPassword = currentState.confirmPassword.trim()

        // 验证
        when {
            cleanAccount.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "账号不能为空") }
                return
            }
            cleanPassword.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "密码不能为空") }
                return
            }
            cleanPassword.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "密码长度至少6位") }
                return
            }
            cleanPassword != cleanConfirmPassword -> {
                _uiState.update { it.copy(errorMessage = "两次密码输入不一致") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.register(cleanAccount, cleanPassword, cleanConfirmPassword)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isRegisterSuccess = true
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
