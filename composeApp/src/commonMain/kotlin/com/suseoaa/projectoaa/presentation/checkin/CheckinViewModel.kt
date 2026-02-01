package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.data.model.CheckinAccountData
import com.suseoaa.projectoaa.data.model.CheckinResult
import com.suseoaa.projectoaa.data.repository.CheckinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 652打卡 UI 状态
 */
data class CheckinUiState(
    val accounts: List<CheckinAccountData> = emptyList(),
    val isLoading: Boolean = false,
    val currentCheckingAccount: CheckinAccountData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // 编辑对话框状态
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: CheckinAccountData? = null,
    // 验证码对话框状态
    val showCaptchaDialog: Boolean = false,
    val captchaImageBytes: ByteArray? = null,
    val isLoadingCaptcha: Boolean = false,
    val isLoggingIn: Boolean = false
)

/**
 * 652打卡 ViewModel
 */
class CheckinViewModel(
    private val repository: CheckinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    /**
     * 加载所有账号
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val accounts = repository.getAllAccounts()
                _uiState.update { it.copy(accounts = accounts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载账号失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 添加账号
     */
    fun addAccount(studentId: String, password: String, name: String = "", remark: String = "") {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            if (repository.isAccountExists(studentId)) {
                _uiState.update { it.copy(errorMessage = "该学号已存在") }
                return@launch
            }

            val result = repository.addAccount(studentId, password, name, remark)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "添加成功", showAddDialog = false) }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "添加失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 更新账号
     */
    fun updateAccount(id: Long, studentId: String, password: String, name: String, remark: String) {
        viewModelScope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            val result = repository.updateAccount(id, studentId, password, name, remark)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "更新成功",
                        showEditDialog = false,
                        editingAccount = null
                    )
                }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "更新失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 删除账号
     */
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            val result = repository.deleteAccount(id)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "删除成功") }
                loadAccounts()
            } else {
                _uiState.update { it.copy(errorMessage = "删除失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    // ==================== 打卡操作（带验证码） ====================

    /**
     * 开始打卡流程 - 显示验证码对话框
     */
    fun startCheckin(account: CheckinAccountData) {
        _uiState.update {
            it.copy(
                currentCheckingAccount = account,
                showCaptchaDialog = true,
                captchaImageBytes = null,
                isLoadingCaptcha = true
            )
        }
        // 获取验证码
        refreshCaptcha()
    }

    /**
     * 刷新验证码
     */
    fun refreshCaptcha() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCaptcha = true) }

            val result = repository.fetchCaptchaImage()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        captchaImageBytes = result.getOrNull(),
                        isLoadingCaptcha = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingCaptcha = false,
                        errorMessage = "获取验证码失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    /**
     * 提交验证码并执行打卡
     */
    fun submitCaptchaAndCheckin(captchaCode: String) {
        val account = _uiState.value.currentCheckingAccount ?: return

        if (captchaCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }

            // 1. 登录
            val loginResult = repository.loginWithCaptcha(
                username = account.studentId,
                password = account.password,
                captchaCode = captchaCode
            )

            if (loginResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = loginResult.exceptionOrNull()?.message ?: "登录失败"
                    )
                }
                // 刷新验证码
                refreshCaptcha()
                return@launch
            }

            // 2. 执行打卡
            val checkinResult = repository.performCheckinAfterLogin(account)
            val message = when (checkinResult) {
                is CheckinResult.Success -> checkinResult.message
                is CheckinResult.AlreadyChecked -> checkinResult.message
                is CheckinResult.NoTask -> checkinResult.message
                is CheckinResult.Failed -> checkinResult.error
            }

            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    showCaptchaDialog = false,
                    currentCheckingAccount = null,
                    captchaImageBytes = null,
                    successMessage = if (checkinResult is CheckinResult.Failed) null else message,
                    errorMessage = if (checkinResult is CheckinResult.Failed) message else null
                )
            }
            loadAccounts() // 刷新状态
        }
    }

    /**
     * 取消打卡
     */
    fun cancelCheckin() {
        _uiState.update {
            it.copy(
                showCaptchaDialog = false,
                currentCheckingAccount = null,
                captchaImageBytes = null,
                isLoadingCaptcha = false,
                isLoggingIn = false
            )
        }
    }

    // ==================== 对话框控制 ====================

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(account: CheckinAccountData) {
        _uiState.update { it.copy(showEditDialog = true, editingAccount = account) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingAccount = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
