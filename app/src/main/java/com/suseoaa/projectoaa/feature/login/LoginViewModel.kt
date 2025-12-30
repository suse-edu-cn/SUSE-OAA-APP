package com.suseoaa.projectoaa.feature.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.model.login.LoginRequest
import com.suseoaa.projectoaa.core.network.login.LoginClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    //    UI状态
    var account by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set


    //   登录方法
    fun login(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (listOf(account, password).any { it.isBlank() }) {
            onError("账号或密码不能为空,$account,$password")
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val request = LoginRequest(username = account, password = password)
                val response = LoginClient.apiService.login(request)
                if (response.code == 200 && response.data?.token != null) {
                    val token = response.data.token
                    tokenManager.saveToken(token)
                    onSuccess(token)
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
//                在控制台打印错误信息
//                e.printStackTrace()
                onError("网络请求失败:${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}