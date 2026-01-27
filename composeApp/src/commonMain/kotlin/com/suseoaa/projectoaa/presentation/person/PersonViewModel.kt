package com.suseoaa.projectoaa.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.AuthRepository
import com.suseoaa.projectoaa.shared.data.repository.Result
import com.suseoaa.projectoaa.shared.data.repository.UserRepository
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
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
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = userRepository.getUserInfo()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            userInfo = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            message = result.message
                        )
                    }
                }
                is Result.Loading -> { /* 不会发生 */ }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
