package com.suseoaa.projectoaa.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.PersonData
import com.suseoaa.projectoaa.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonUiState(
    val isLoading: Boolean = false,
    val userInfo: PersonData? = null,
    val isLoggedOut: Boolean = false,
    val message: String? = null,
    val isCheckinUnlocked: Boolean = false  // 652签到功能是否已解锁
)

class PersonViewModel(
    private val personRepository: PersonRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        loadCheckinUnlockStatus()
    }
    
    /**
     * 加载652签到功能解锁状态
     */
    private fun loadCheckinUnlockStatus() {
        viewModelScope.launch {
            tokenManager.checkinUnlockedFlow.collect { unlocked ->
                _uiState.update { it.copy(isCheckinUnlocked = unlocked) }
            }
        }
    }
    
    /**
     * 解锁652签到功能（永久保存）
     */
    fun unlockCheckinFeature() {
        viewModelScope.launch {
            tokenManager.unlockCheckinFeature()
            _uiState.update { it.copy(isCheckinUnlocked = true) }
        }
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = personRepository.getPersonInfo()
            
            result.onSuccess { data ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        userInfo = data
                    )
                }
            }
            
            result.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            personRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun updateInfo(username: String, name: String) {
        if (username.isBlank() || name.isBlank()) {
            _uiState.update { it.copy(message = "用户名或姓名不能为空") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = personRepository.updateUserInfo(username, name)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "信息更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }
            
            result.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun uploadAvatar(imageData: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在上传头像...") }
            
            val result = personRepository.uploadAvatar(imageData)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "头像更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }
            
            result.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "头像上传失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
