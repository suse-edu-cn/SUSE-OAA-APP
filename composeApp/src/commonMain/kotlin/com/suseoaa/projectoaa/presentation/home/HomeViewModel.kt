package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.AnnouncementData
import com.suseoaa.projectoaa.data.model.PersonData
import com.suseoaa.projectoaa.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.data.repository.PersonRepository
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
    private val announcementRepository: AnnouncementRepository,
    private val personRepository: PersonRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 部门列表
    val departments = announcementRepository.departments

    // 用户信息缓存
    private var currentUserInfo: PersonData? = null

    init {
        preloadAllDepartments()
        fetchUserProfile()
    }

    // ================== 平板与导航逻辑 ==================

    fun initForTablet() {
        if (_uiState.value.selectedDepartment == null) {
            selectDepartment(departments.first())
        }
    }

    fun selectDepartment(department: String) {
        _uiState.update { it.copy(selectedDepartment = department) }
        fetchDetailInfo(department)
    }

    // ================== 用户信息 ==================

    private fun fetchUserProfile() {
        viewModelScope.launch {
            personRepository.getPersonInfo().onSuccess { user ->
                currentUserInfo = user
                _uiState.update { it.copy(userInfo = user) }
                _uiState.value.selectedDepartment?.let { checkEditPermission(it) }
            }
        }
    }

    fun loadUserInfo() {
        fetchUserProfile()
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
                    result.onSuccess { newMap[dept] = it }
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
            val result = announcementRepository.fetchAnnouncementInfo(department)

            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailData = info,
                        editContent = info.data,
                        cardInfos = it.cardInfos + (department to info)
                    )
                }
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailError = e.message
                    )
                }
            }
        }
    }

    private fun checkEditPermission(targetDepartment: String) {
        val user = currentUserInfo ?: return
        val role = user.role
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

            val result = announcementRepository.updateAnnouncementInfo(dept, content)

            result.onSuccess {
                fetchDetailInfo(dept)
                _uiState.update { it.copy(isUpdating = false) }
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        detailError = "更新失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ================== 其他 ==================

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun logout() {
        viewModelScope.launch {
            personRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, detailError = null) }
    }
}

