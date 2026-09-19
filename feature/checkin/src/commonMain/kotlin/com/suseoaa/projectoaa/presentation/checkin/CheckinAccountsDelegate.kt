package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.domain.repository.CheckinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 签到账号的增删改查、筛选与对话框状态。
 *
 * 从 CheckinViewModel 里拆出来的第一块。选它先动，是因为它与登录会话状态
 * （已登录学号、当前登录入口、轮询 Job）完全无关，可以独立搬走；而登录、
 * 验证码、短信、扫码那几条流程共享这些可变状态，交织很深，需要先有测试覆盖
 * 才谈得上拆。
 *
 * 直接共享 ViewModel 的 [uiState]：账号列表与对话框开关本来就属于同一份界面
 * 状态，硬拆成两份反而要做同步。
 */
internal class CheckinAccountsDelegate(
    private val uiState: MutableStateFlow<CheckinUiState>,
    private val scope: CoroutineScope,
    private val repository: CheckinRepository,
) {

    fun loadAccounts() {
        scope.launch {
            uiState.update { it.copy(isLoading = true) }
            try {
                val accounts = repository.getAllAccounts()
                uiState.update { it.copy(accounts = accounts, isLoading = false) }
            } catch (e: Exception) {
                uiState.update {
                    it.copy(isLoading = false, errorMessage = "加载账号失败: ${e.message}")
                }
            }
        }
    }

    fun setAccountFilter(filter: AccountFilterType) {
        uiState.update { it.copy(accountFilter = filter) }
    }

    fun filteredAccounts(): List<CheckinAccountData> = uiState.value.filteredAccounts

    fun addAccount(
        studentId: String,
        password: String,
        name: String = "",
        remark: String = "",
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name,
    ) {
        scope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }
            if (repository.isAccountExists(studentId)) {
                uiState.update { it.copy(errorMessage = "该学号已存在") }
                return@launch
            }

            val result = repository.addAccount(studentId, password, name, remark, selectedLocation)
            if (result.isSuccess) {
                uiState.update { it.copy(successMessage = "添加成功", showAddDialog = false) }
                loadAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "添加失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun updateAccount(
        id: Long,
        studentId: String,
        password: String,
        name: String,
        remark: String,
        selectedLocation: String = CheckinLocations.DEFAULT_CAMPUS.name,
    ) {
        scope.launch {
            if (studentId.isBlank() || password.isBlank()) {
                uiState.update { it.copy(errorMessage = "学号和密码不能为空") }
                return@launch
            }

            val result = repository.updateAccount(
                id, studentId, password, name, remark, selectedLocation
            )
            if (result.isSuccess) {
                uiState.update {
                    it.copy(successMessage = "更新成功", showEditDialog = false, editingAccount = null)
                }
                loadAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "更新失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun deleteAccount(id: Long) {
        scope.launch {
            val result = repository.deleteAccount(id)
            if (result.isSuccess) {
                uiState.update { it.copy(successMessage = "删除成功") }
                loadAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "删除失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun updateLocation(accountId: Long, locationName: String) {
        scope.launch {
            val result = repository.updateLocation(accountId, locationName)
            if (result.isSuccess) {
                uiState.update { it.copy(successMessage = "签到地点已更新") }
                loadAccounts()
            } else {
                uiState.update {
                    it.copy(errorMessage = "更新失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    // ==================== 对话框与提示 ====================

    fun showAddDialog() = uiState.update { it.copy(showAddDialog = true) }

    fun hideAddDialog() = uiState.update { it.copy(showAddDialog = false) }

    fun showEditDialog(account: CheckinAccountData) =
        uiState.update { it.copy(showEditDialog = true, editingAccount = account) }

    fun hideEditDialog() =
        uiState.update { it.copy(showEditDialog = false, editingAccount = null) }

    fun clearMessages() =
        uiState.update { it.copy(errorMessage = null, successMessage = null) }
}
