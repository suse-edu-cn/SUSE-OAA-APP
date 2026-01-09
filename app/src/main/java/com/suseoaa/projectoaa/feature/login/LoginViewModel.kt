package com.suseoaa.projectoaa.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.login.LoginService
import com.suseoaa.projectoaa.core.network.model.login.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val loginService: LoginService
) : ViewModel() {

    // UI状态
    var account by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set

    // 登录方法
    fun login(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val cleanAccount = account.trim()
        val cleanPassword = password.trim()

        if (cleanAccount.isBlank() || cleanPassword.isBlank()) {
            onError("账号或密码不能为空")
            return
        }

        viewModelScope.launch {
            isLoading = true
            try {
                val request = LoginRequest(username = cleanAccount, password = cleanPassword)
                // [修改] 使用注入的 Service
                val response = loginService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200 && body.data?.token != null) {
                        val token = body.data.token
                        tokenManager.saveToken(token)
                        onSuccess(token)
                    } else {
                        onError(body?.message ?: "登录失败: 未知错误")
                    }
                } else {
                    // [修改] 处理 401 或其他错误码
                    if (response.code() == 401) {
                        onError("登录失败：账号或密码错误")
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: ""
                        onError("请求失败(${response.code()}): $errorMsg")
                    }
                }
            } catch (e: Exception) {
                // 在控制台打印错误信息
                e.printStackTrace()
                onError("网络请求失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}