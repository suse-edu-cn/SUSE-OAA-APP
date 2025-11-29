package com.suseoaa.projectoaa.login.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.network.NetworkModule
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.UpdatePasswordRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import com.suseoaa.projectoaa.login.model.UpdateUserInfoRequest

class ProfileViewModel : BaseViewModel() {

    var userInfo by mutableStateOf<UserInfoData?>(null)
        private set

    // === 编辑模式状态 ===
    var isEditing by mutableStateOf(false)
        private set

    // === 暂存编辑中的数据 ===
    var editName by mutableStateOf("")
    var editStudentId by mutableStateOf("")
    var editUsername by mutableStateOf("")
    var editRole by mutableStateOf("") // 这里将用于下拉选择
    var editDepartment by mutableStateOf("")
    var showPasswordDialog by mutableStateOf(false)
    var newPasswordInput by mutableStateOf("")
    private val profileApi by lazy {
        NetworkModule.createService(ApiService::class.java)
    }

    fun fetchUserInfo() {
        launchDataLoad {
            val token = SessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效，请重新登录")

            val response = profileApi.getUserInfo(token)

            if (response.isSuccessful && response.body()?.code == 200) {
                userInfo = response.body()?.data
            } else {
                throw Exception(response.body()?.message ?: "获取信息失败")
            }
        }
    }

    // 进入编辑模式，回填数据
    fun startEditing() {
        userInfo?.let {
            editName = it.name
            editStudentId = it.studentid.toString()
            editUsername = it.username
            editRole = it.role
            editDepartment = it.department
            isEditing = true
        }
    }

    // 取消编辑
    fun cancelEditing() {
        isEditing = false
    }

    fun updatePassword(context: Context, onSuccess:()-> Unit) {
        if (newPasswordInput.isBlank()) {
            errorMessage = "密码不能为空"
            return
        }

        launchDataLoad {
            val token = SessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdatePasswordRequest(oldPassword = "", newPassword = newPasswordInput)

            val response = profileApi.updatePassword(token, request)

            // 3. 处理响应
            if (response.isSuccessful && response.body()?.code == 200) {
                showPasswordDialog = false
                newPasswordInput = ""
                logout(context)
                onSuccess()
            } else {
                throw Exception(response.body()?.message ?: "修改密码失败")
            }
        }
    }

    // 提交保存
    fun saveUserInfo() {
        launchDataLoad {
            val token = SessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdateUserInfoRequest(
                studentid = editStudentId.toLongOrNull() ?: 0L,
//                username = editUsername,
                name = editName,
                role = editRole,
                department = editDepartment
            )

            val response = profileApi.updateUserInfo(token, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                isEditing = false
                fetchUserInfo()
            } else {
                throw Exception(response.body()?.message ?: "修改失败")
            }
        }
    }

    fun logout(context: Context) {
        // 1. 清除本地所有 Token 和数据
        SessionManager.clear(context)
        // 2. 清空当前 ViewModel 的数据
        userInfo = null
    }
}