package com.suseoaa.projectoaa.login.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class MainViewModel : BaseViewModel() {

    // UI 状态文本 (用于显示 "登录成功"、"密码错误" 等具体消息)
    var uiState by mutableStateOf("")
        private set

    // 登录成功标志 (触发 UI 跳转)
    var loginSuccess by mutableStateOf(false)
        private set

    // 自动登录检查结果
    var isTokenValid by mutableStateOf<Boolean?>(null)
        private set

    // 引入仓库
    private val repository = AuthRepository()

    /**
     * 启动时检查 Token
     */
    fun checkToken(context: Context) {
        viewModelScope.launch {
            delay(500)
            val token = SessionManager.fetchToken(context)
            if (!token.isNullOrBlank()) {
                Log.d("Auth", "本地Token有效: ${SessionManager.currentUser}")
                isTokenValid = true
            } else {
                isTokenValid = false
            }
        }
    }

    /**
     * 登录逻辑
     */
    fun login(context: Context, username: String, pass: String) {
        // 使用基类的 launchDataLoad 自动处理 isLoading 和 异常捕获
        launchDataLoad {
            Log.d("LoginDebug", "请求已发送")
            uiState = "正在登录..."
            loginSuccess = false
            try {
                val result = withTimeout(5000L) {repository.login(username, pass)}

                result.onSuccess { token ->
                    uiState = "登录成功"
                    SessionManager.saveToken(context, token)
                    SessionManager.saveUserInfo(context, username, "会员")
                    loginSuccess = true
                }.onFailure { error ->
                    uiState = "登录失败: ${error.message}"
                }
            } catch (e: TimeoutCancellationException){
                Log.e("LoginDebug", "登录超时")
                uiState = "登录失败：Timeout"
                loginSuccess = false
            }
        }
    }

    /**
     * 注册逻辑
     */
    fun register(studentid: String, name: String, username: String, pass: String, role: String) {
        launchDataLoad {
            Log.d("RegisterDebug", "请求已发送")
            uiState = "正在注册..."

            val request = RegisterRequest(
                studentid = studentid,
                name = name,
                username = username,
                password = pass,
                role = role
            )
            try {
                val result = withTimeout(5000L){repository.register(request)}

                result.onSuccess { msg ->
                    uiState = "注册成功: $msg"
                }.onFailure { error ->
                    uiState = "注册失败: ${error.message}"
                }
            }catch (e: TimeoutCancellationException){
                Log.e("RegisterDebug", "注册超时")
                uiState="注册失败：Timeout"
            }

        }
    }

    /**
     * 清除状态 (用于页面跳转离开时)
     */
    fun clearState() {
        uiState = ""
        loginSuccess = false
        isTokenValid = null
    }
}