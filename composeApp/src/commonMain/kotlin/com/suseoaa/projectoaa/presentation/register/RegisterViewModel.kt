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
    val studentId: String = "",      // 学号
    val realName: String = "",       // 姓名
    val userName: String = "",       // 用户名
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

    fun updateStudentId(studentId: String) {
        if (studentId.length <= 11) {
            _uiState.update { it.copy(studentId = studentId, errorMessage = null) }
        }
    }

    fun updateRealName(realName: String) {
        if (realName.length <= 20) {
            _uiState.update { it.copy(realName = realName, errorMessage = null) }
        }
    }

    fun updateUserName(userName: String) {
        if (userName.length <= 20) {
            _uiState.update { it.copy(userName = userName, errorMessage = null) }
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
        val cleanStudentId = currentState.studentId.trim()
        val cleanRealName = currentState.realName.trim()
        val cleanUserName = currentState.userName.trim()
        val cleanPassword = currentState.password.trim()
        val cleanConfirmPassword = currentState.confirmPassword.trim()

        // 验证
        when {
            cleanStudentId.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "学号不能为空") }
                return
            }
            cleanRealName.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "姓名不能为空") }
                return
            }
            cleanUserName.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "用户名不能为空") }
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

            // 使用 studentId 作为注册参数
            when (val result = authRepository.register(cleanStudentId, cleanPassword, cleanConfirmPassword)) {
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
