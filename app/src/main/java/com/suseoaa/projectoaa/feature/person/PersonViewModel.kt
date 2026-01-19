package com.suseoaa.projectoaa.feature.person

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.PersonRepository
import com.suseoaa.projectoaa.core.network.model.person.Data
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val repository: PersonRepository
) : ViewModel() {

    private val _personInfo = MutableStateFlow<Data?>(null)
    val personInfo = _personInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _uiEvent = MutableStateFlow<String?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    init {
        fetchPersonInfo()
    }

    fun fetchPersonInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getPersonInfo()
            result.onSuccess {
                _personInfo.value = it
            }.onFailure {
                // _uiEvent.value = "获取信息失败: ${it.message}" // 可选：静默失败不弹窗
            }
            _isLoading.value = false
        }
    }

    fun updateInfo(username: String, name: String) {
        if (username.isBlank() || name.isBlank()) {
            _uiEvent.value = "用户名或姓名不能为空"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateUserInfo(username, name)
            result.onSuccess {
                _uiEvent.value = "信息修改成功"
                fetchPersonInfo()
            }.onFailure {
                _uiEvent.value = "修改失败: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiEvent.value = "正在处理图片..."
            val result = repository.uploadAvatar(uri)
            result.onSuccess {
                _uiEvent.value = "头像更新成功"
                fetchPersonInfo()
            }.onFailure {
                _uiEvent.value = "头像上传失败: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onSuccess()
        }
    }

    fun clearUiEvent() {
        _uiEvent.value = null
    }
}