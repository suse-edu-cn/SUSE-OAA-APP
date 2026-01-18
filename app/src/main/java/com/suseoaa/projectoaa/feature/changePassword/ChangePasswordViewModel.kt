package com.suseoaa.projectoaa.feature.changePassword


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val personRepository: PersonRepository
) : ViewModel() {

    var oldPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var uiState by mutableStateOf(ChangePasswordUiState())
        private set

    fun updateOldPassword(input: String) {
        oldPassword = input
        uiState = uiState.copy(error = null) // 输入时清除错误
    }

    fun updateNewPassword(input: String) {
        newPassword = input
        uiState = uiState.copy(error = null)
    }

    fun updateConfirmPassword(input: String) {
        confirmPassword = input
        uiState = uiState.copy(error = null)
    }

    fun submitChangePassword(onSuccess: () -> Unit) {
        // 1. 本地校验
        if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            uiState = uiState.copy(error = "请填写完整信息")
            return
        }
        if (newPassword != confirmPassword) {
            uiState = uiState.copy(error = "两次输入的密码不一致")
            return
        }
        if (newPassword.length < 6) { // 假设最小长度
            uiState = uiState.copy(error = "新密码长度不能少于6位")
            return
        }

        // 2. 发起请求
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = personRepository.changePassword(oldPassword, newPassword)

            result.onSuccess { msg ->
                uiState = uiState.copy(isLoading = false, successMessage = msg)
                // 成功后，通常需要退出登录或返回
                personRepository.logout() // 可选：修改密码后强制登出
                onSuccess()
            }.onFailure { e ->
                uiState = uiState.copy(isLoading = false, error = e.message ?: "修改失败")
            }
        }
    }
}

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)