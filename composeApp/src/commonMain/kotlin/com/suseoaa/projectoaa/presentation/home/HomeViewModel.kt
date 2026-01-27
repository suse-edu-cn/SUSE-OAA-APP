package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.data.repository.AuthRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.data.repository.UserRepository
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
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
    val cardInfos: Map<String, String> = emptyMap()  // 部门 -> 公告内容
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

    init {
        loadUserInfo()
        loadDepartmentAnnouncements()
    }

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

    private fun loadDepartmentAnnouncements() {
        viewModelScope.launch {
            val announcements = announcementRepository.fetchAllDepartmentAnnouncements()
            val cardInfos = announcements.mapValues { it.value?.data ?: "" }
            _uiState.update { it.copy(cardInfos = cardInfos) }
        }
    }

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
        _uiState.update { it.copy(errorMessage = null) }
    }
}
