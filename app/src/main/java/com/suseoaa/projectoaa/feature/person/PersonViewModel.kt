package com.suseoaa.projectoaa.feature.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.PersonRepository
import com.suseoaa.projectoaa.core.network.model.person.Data
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val repository: PersonRepository
) : ViewModel() {
    //UI状态流
    private val _userInfo = MutableStateFlow<Data?>(null)
    val userInfo: StateFlow<Data?> = _userInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        fetchPersonInfo()
    }

    fun fetchPersonInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getPersonInfo()

            result.onSuccess { data ->
                _userInfo.value = data
            }.onFailure { e ->
                _errorMessage.value = e.message
            }

            _isLoading.value = false
        }
    }
}