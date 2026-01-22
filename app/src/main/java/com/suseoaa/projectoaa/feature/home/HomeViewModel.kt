package com.suseoaa.projectoaa.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.core.data.repository.PersonRepository
import com.suseoaa.projectoaa.core.network.model.announcement.FetchAnnouncementInfoResponse
import com.suseoaa.projectoaa.core.network.model.person.Data
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnnouncementRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    // 1. 部门列表
    val departments = listOf("协会", "算法竞赛部", "项目实践部", "组织宣传部", "理事会", "秘书处")

    // 2. UI 状态
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // 用户信息缓存
    private var currentUserInfo: Data? = null

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

    fun clearSelection() {
        _uiState.update { it.copy(selectedDepartment = null) }
    }

    // ================== 业务与权限逻辑 ==================

    private fun fetchUserProfile() {
        viewModelScope.launch {
            personRepository.getPersonInfo().onSuccess { user ->
                currentUserInfo = user
                _uiState.value.selectedDepartment?.let { checkEditPermission(it) }
            }
        }
    }

    private fun checkEditPermission(targetDepartment: String) {
        val user = currentUserInfo ?: return
        val role = user.role
        val myDepartment = user.department

        // 规则：会长/副会长/开发者 -> 可修改所有
        val canEdit = when {
            role in listOf("会长", "副会长", "开发者") -> true
            targetDepartment == myDepartment && role != "会员" -> true
            else -> false
        }

        _uiState.update { it.copy(canEditCurrent = canEdit) }
    }

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
            val result = repository.fetchAnnouncementInfo(department)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailData = info,
                        editContent = info.data,
                        cardInfos = it.cardInfos + (department to info)
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingDetail = false, detailError = e.message) }
            }
        }
    }

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

            val result = repository.updateAnnouncementInfo(dept, content)

            result.onSuccess {
                fetchDetailInfo(dept)
                _uiState.update { it.copy(isUpdating = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        detailError = "更新失败: ${e.message}"
                    )
                }
            }
        }
    }

    private fun preloadAllDepartments() {
        viewModelScope.launch {
            val deferreds = departments.map { dept ->
                async { dept to repository.fetchAnnouncementInfo(dept) }
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
}

data class HomeUiState(
    val selectedDepartment: String? = null,
    val cardInfos: Map<String, FetchAnnouncementInfoResponse.Data> = emptyMap(),
    val isLoadingDetail: Boolean = false,
    val detailData: FetchAnnouncementInfoResponse.Data? = null,
    val detailError: String? = null,
    val canEditCurrent: Boolean = false,
    val showEditDialog: Boolean = false,
    val editContent: String = "",
    val isUpdating: Boolean = false
)