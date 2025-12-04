package com.suseoaa.projectoaa.login.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.model.RegisterRequest
import com.suseoaa.projectoaa.login.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    // UI 状态文本 (用于显示 "登录成功"、"密码错误" 等具体消息)
    var uiState by mutableStateOf("")
        private set

    // 登录成功标志 (触发 UI 跳转)
    var loginSuccess by mutableStateOf(false)
        private set

    // 自动登录/Token检查结果 (null=检查中, true=有效, false=无效)
    var isTokenValid by mutableStateOf<Boolean?>(null)
        private set

    /**
     * 启动时检查 Token 有效性
     * 策略：
     * 1. 检查本地是否有 Token。
     * 2. 如果有，调用后端接口(getUserInfo)验证其是否过期/有效。
     * 3. 如果有效，更新用户信息并允许进入；如果无效，强制登录。
     */
    fun checkToken() {
        viewModelScope.launch {
            // 给一点点延迟，让 Splash 动画展示一下，避免闪屏
            delay(500)

            val localToken = sessionManager.jwtToken
            if (localToken.isNullOrBlank()) {
                Log.d("Auth", "本地无Token，需登录")
                isTokenValid = false
                return@launch
            }

            // 尝试联网验证 Token
            try {
                // 使用 getUserInfo 来验证 Token 是否有效
                // 注意：这里假设 repository.getUserInfo 内部会处理 401 错误并返回 failure
                val result = withTimeout(5000L) {
                    repository.getUserInfo(localToken)
                }

                result.onSuccess { userInfo ->
                    Log.d("Auth", "Token验证成功，用户: ${userInfo.name}")
                    // 更新本地缓存的用户信息（确保是最新的）
                    sessionManager.saveUserInfo(userInfo.username, userInfo.role)
                    isTokenValid = true
                }.onFailure { e ->
                    Log.w("Auth", "Token验证失败或过期: ${e.message}")
                    // Token 失效，清除本地数据，要求重新登录
                    sessionManager.clear()
                    isTokenValid = false
                }
            } catch (e: Exception) {
                // 网络超时或其他异常处理
                Log.e("Auth", "Token验证过程异常: ${e.message}")

                // 策略选择：
                // A. 严格模式：网络错误视为验证失败 -> isTokenValid = false
                // B. 宽松模式（离线支持）：网络错误但本地有 Token -> 暂时信任本地 Token -> isTokenValid = true

                // 这里采用宽松模式，允许离线进入（根据你的业务需求可改为 false）
                if (e is TimeoutCancellationException) {
                    // 提示：网络连接超时，进入离线模式
                    isTokenValid = true
                } else {
                    // 其他错误（如格式错误），视为无效
                    isTokenValid = false
                }
            }
        }
    }

    /**
     * 登录逻辑
     */
    fun login(username: String, pass: String) {
        launchDataLoad {
            Log.d("LoginDebug", "请求已发送")
            uiState = "正在登录..."
            loginSuccess = false
            try {
                val result = withTimeout(5000L) { repository.login(username, pass) }

                result.onSuccess { token ->
                    uiState = "登录成功"
                    // 保存 Token
                    sessionManager.saveToken(token)
                    // 登录成功后，立即获取一次用户信息以保存角色等数据
                    try {
                        val userResult = repository.getUserInfo(token)
                        userResult.onSuccess { u ->
                            sessionManager.saveUserInfo(username, u.role)
                        }
                    } catch (e: Exception) {
                        // 获取用户信息失败不影响登录流程，使用默认值
                        sessionManager.saveUserInfo(username, "会员")
                    }

                    loginSuccess = true
                }.onFailure { error ->
                    uiState = "登录失败: ${error.message}"
                }
            } catch (e: TimeoutCancellationException) {
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
                student_id = studentid,
                name = name,
                username = username,
                password = pass,
                role = role
            )
            try {
                val result = withTimeout(5000L) { repository.register(request) }

                result.onSuccess { msg ->
                    uiState = "注册成功: $msg"
                }.onFailure { error ->
                    uiState = "注册失败: ${error.message}"
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("RegisterDebug", "注册超时")
                uiState = "注册失败：Timeout"
            }
        }
    }

    /**
     * 登出逻辑
     */
    fun logout() {
        sessionManager.clear()
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