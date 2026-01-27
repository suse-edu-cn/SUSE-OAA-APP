package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.data.repository.AuthRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.data.repository.UserRepository
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementData
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页界面状态
 */
data class HomeUiState(
    val userInfo: PersonData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTabIndex: Int = 0,
    val isLoggedOut: Boolean = false,
    // 首页卡片数据
    val cardInfos: Map<String, AnnouncementData?> = emptyMap(),
    // 详情页状态
    val selectedDepartment: String? = null,
    val isLoadingDetail: Boolean = false,
    val detailData: AnnouncementData? = null,
    val detailError: String? = null,
    // 编辑状态
    val canEditCurrent: Boolean = false,
    val showEditDialog: Boolean = false,
    val editContent: String = "",
    val isUpdating: Boolean = false
)

/**
 * 首页 ViewModel
 */
class HomeViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 部门列表
    val departments = announcementRepository.departments

    init {
        loadUserInfo()
        preloadAllDepartments()
    }

    // ================== 用户信息 ==================

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = userRepository.getUserInfo()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            userInfo = result.data,
                            errorMessage = null
                        )
                    }
                    // 如果已选择部门，检查权限
                    _uiState.value.selectedDepartment?.let { checkEditPermission(it) }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    // ================== 公告数据加载 ==================

    private fun preloadAllDepartments() {
        viewModelScope.launch {
            val deferreds = departments.map { dept ->
                async { dept to announcementRepository.fetchAnnouncementInfo(dept) }
            }
            val results = deferreds.awaitAll()

            _uiState.update { currentState ->
                val newMap = currentState.cardInfos.toMutableMap()
                results.forEach { (dept, result) ->
                    when (result) {
                        is Result.Success -> newMap[dept] = result.data
                        else -> newMap[dept] = null
                    }
                }
                currentState.copy(cardInfos = newMap)
            }
        }
    }

    // ================== 详情页功能 ==================

    fun fetchDetailInfo(department: String) {
        _uiState.update {
            it.copy(
                selectedDepartment = department,
                isLoadingDetail = true,
                detailError = null
            )
        }
        checkEditPermission(department)

        viewModelScope.launch {
            when (val result = announcementRepository.fetchAnnouncementInfo(department)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            detailData = result.data,
                            editContent = result.data.data,
                            cardInfos = it.cardInfos + (department to result.data)
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            detailError = result.message
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    private fun checkEditPermission(targetDepartment: String) {
        val user = _uiState.value.userInfo ?: return
        val role = user.role ?: ""
        val myDepartment = user.department ?: ""

        // 规则：会长/副会长/开发者 -> 可修改所有
        val canEdit = when {
            role in listOf("会长", "副会长", "开发者") -> true
            targetDepartment == myDepartment && role != "会员" -> true
            else -> false
        }

        _uiState.update { it.copy(canEditCurrent = canEdit) }
    }

    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedDepartment = null,
                detailData = null,
                detailError = null
            ) 
        }
    }

    // ================== 编辑功能 ==================

    fun onEditContentChange(newContent: String) {
        _uiState.update { it.copy(editContent = newContent) }
    }

    fun toggleEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun submitUpdate() {
        val dept = _uiState.value.selectedDepartment ?: return
        val content = _uiState.value.editContent

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, showEditDialog = false) }

            when (val result = announcementRepository.updateAnnouncementInfo(dept, content)) {
                is Result.Success -> {
                    // 刷新详情
                    fetchDetailInfo(dept)
                    _uiState.update { it.copy(isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            detailError = "更新失败: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    // ================== 其他 ==================

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, detailError = null) }
    }
}
