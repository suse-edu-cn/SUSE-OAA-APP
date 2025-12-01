package com.suseoaa.projectoaa.login.viewmodel

// [修复] 移除了 Context 导入
// import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
// [修复] 导入 Hilt 注解
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

// [修复] 1. 添加 Hilt 注解
@HiltViewModel
class MainViewModel @Inject constructor(
    // [修复] 2. 注入 Repository 和 SessionManager
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    // UI 状态文本 (用于显示 "登录成功"、"密码错误" 等具体消息)
    var uiState by mutableStateOf("")
        private set

    // 登录成功标志 (触发 UI 跳转)
    var loginSuccess by mutableStateOf(false)
        private set

    // 自动登录检查结果
    var isTokenValid by mutableStateOf<Boolean?>(null)
        private set

    // [修复] 移除了 'private val repository = AuthRepository()'

    /**
     * 启动时检查 Token
     * [修复] 移除了 context 参数
     */
    fun checkToken() {
        viewModelScope.launch {
            delay(500)
            // [修复] SessionManager 已经在 init 时自动加载了 Token
            if (sessionManager.isLoggedIn()) {
                Log.d("Auth", "本地Token有效: ${sessionManager.currentUser}")
                isTokenValid = true
            } else {
                isTokenValid = false
            }
        }
    }

    /**
     * 登录逻辑
     * [修复] 移除了 context 参数
     */
    fun login(username: String, pass: String) {
        // 使用基类的 launchDataLoad 自动处理 isLoading 和 异常捕获
        launchDataLoad {
            Log.d("LoginDebug", "请求已发送")
            uiState = "正在登录..."
            loginSuccess = false
            try {
                // repository 现在是注入的实例
                val result = withTimeout(5000L) {repository.login(username, pass)}

                result.onSuccess { token ->
                    uiState = "登录成功"
                    // [修复] 调用注入的 sessionManager 实例 (无 context)
                    sessionManager.saveToken(token)
                    sessionManager.saveUserInfo(username, "会员")
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
                // repository 现在是注入的实例
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
     * [新增]
     * 将登出逻辑封装到 ViewModel 中
     * (这是 Navigation.kt 所需的)
     */
    fun logout() {
        sessionManager.clear() // 调用注入的 sessionManager 实例
        clearState()
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