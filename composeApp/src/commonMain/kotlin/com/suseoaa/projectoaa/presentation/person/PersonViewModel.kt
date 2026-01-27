package com.suseoaa.projectoaa.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val message: String? = null
)

class PersonViewModel(
    private val personRepository: PersonRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
