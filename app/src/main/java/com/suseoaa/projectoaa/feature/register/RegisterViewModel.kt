package com.suseoaa.projectoaa.feature.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.network.model.register.RegisterErrorResponse
import com.suseoaa.projectoaa.core.network.model.register.RegisterRequest
import com.suseoaa.projectoaa.core.network.register.RegisterClient
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class RegisterViewModel : ViewModel() {
    var studentID by mutableStateOf("")
    var realName by mutableStateOf("")
    var userName by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun register(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (listOf(studentID, realName, userName, password).any { it.isBlank() }) {
            onError("请填写所有选项")
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val request = RegisterRequest(
                    studentId = studentID,
                    name = realName,
                    username = userName,
                    password = password
                )
                val response = RegisterClient.apiService.register(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        onSuccess(body.message)
                    } else {
                        onError(body?.message ?: "注册失败")
                    }
                } else {
                    val errorBody=response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()){
                        try {
                            val errorObj=Json.decodeFromString<RegisterErrorResponse>(errorBody)
                            onError(errorObj.message)
                        }catch (e: Exception){
                            onError("服务器异常:$e")
                        }
                    }
                }
            } catch (e: Exception) {
                onError("网络异常")
            } finally {
                isLoading = false
            }
        }
    }
}