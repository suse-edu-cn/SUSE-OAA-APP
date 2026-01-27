package com.suseoaa.projectoaa.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.OaaAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录界面状态
 */
data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

/**
 * 登录 ViewModel
 */
class LoginViewModel(
    private val authRepository: OaaAuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateAccount(account: String) {
        if (account.length <= 20) {
            _uiState.update { it.copy(account = account, errorMessage = null) }
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        val cleanAccount = currentState.account.trim()
        val cleanPassword = currentState.password.trim()

        if (cleanAccount.isBlank() || cleanPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "账号或密码不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(cleanAccount, cleanPassword)
            
            result.onSuccess { response ->
                // 保存 Token 和学号
                response.data?.token?.let { token ->
                    tokenManager.saveToken(token)
                }
                tokenManager.saveCurrentStudentId(cleanAccount)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoginSuccess = true
                    )
                }
            }
            
            result.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "登录失败"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }
}
