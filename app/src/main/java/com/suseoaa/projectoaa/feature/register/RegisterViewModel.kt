package com.suseoaa.projectoaa.feature.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.network.model.register.RegisterErrorResponse
import com.suseoaa.projectoaa.core.network.model.register.RegisterRequest
import com.suseoaa.projectoaa.core.network.register.RegisterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerService: RegisterService
) : ViewModel() {

    var studentID by mutableStateOf("")
    var realName by mutableStateOf("")
    var userName by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun register(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (listOf(studentID, realName, userName, password,confirmPassword).any { it.isBlank() }) {
            onError("请填写所有选项")
            return
        }

        viewModelScope.launch {
            isLoading = true
            try {
                if (password!=confirmPassword){
                    onError("两次密码输入不一致")
                    return@launch
                }
                val request = RegisterRequest(
                    studentId = studentID,
                    name = realName,
                    username = userName,
                    password = password
                )
                val response = registerService.register(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        onSuccess(body.message)
                    } else {
                        onError(body?.message ?: "注册失败")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        try {
                            // 尝试解析标准错误格式
                            val errorObj = Json.decodeFromString<RegisterErrorResponse>(errorBody)
                            onError(errorObj.message)
                        } catch (e: Exception) {
                            onError("服务器返回错误: ${response.code()}")
                        }
                    } else {
                        onError("注册失败: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                onError("网络异常: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}