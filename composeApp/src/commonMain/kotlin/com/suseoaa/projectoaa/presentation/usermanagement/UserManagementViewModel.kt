package com.suseoaa.projectoaa.presentation.usermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.PersonRepository
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val currentUser: PersonData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val users: List<UserQueryData> = emptyList(),
    // Filter conditions
    val filterDepartment: String = "",
    val filterName: String = "",
    val filterRole: String = "",
    // Update State
    val isUpdating: Boolean = false,
    val updateMessage: String? = null
)

class UserManagementViewModel(
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            personRepository.getPersonInfo().onSuccess { user ->
                _uiState.update { it.copy(currentUser = user) }
                fetchUsers() // 默认请求全部
            }
        }
    }

    fun updateFilters(department: String? = null, name: String? = null, role: String? = null) {
        _uiState.update {
            it.copy(
                filterDepartment = department ?: it.filterDepartment,
                filterName = name ?: it.filterName,
                filterRole = role ?: it.filterRole
            )
        }
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            val result = personRepository.queryUsers(
                department = state.filterDepartment,
                name = state.filterName,
                role = state.filterRole
            )
            result.onSuccess { data ->
                val sortedData = data.sortedByDescending { ranks[it.role] ?: -1 }
                _uiState.update { it.copy(isLoading = false, users = sortedData) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    // 权限等级字典
    val ranks = mapOf(
        "会员" to -1,
        "普通成员" to -1,
        "干事" to 0,
        "副部长" to 1,
        "部长" to 2,
        "会长" to 3,
        "副会长" to 3,
        "开发者" to 4
    )

    // 权限校验逻辑
    fun canEditUser(targetUserRole: String): Boolean {
        val currentRole = _uiState.value.currentUser?.role ?: return false
        // 定制职位（不在预设职位列表中的），默认等同于会长的权限等级(3)
        val currentRank = ranks[currentRole] ?: 3
        val targetRank = ranks[targetUserRole] ?: 3

        return currentRank > targetRank
    }

    fun updateUsers(usersToUpdate: List<UserQueryData>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateMessage = null) }
            val result = personRepository.changeUserMessage(usersToUpdate)
            result.onSuccess { msg ->
                _uiState.update { it.copy(isUpdating = false, updateMessage = msg) }
                fetchUsers() // 刷新数据
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        updateMessage = "更新失败: ${error.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(updateMessage = null, error = null) }
    }
}
