package com.suseoaa.projectoaa.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.UpdatePasswordRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import com.suseoaa.projectoaa.login.model.UpdateUserInfoRequest
import com.suseoaa.projectoaa.login.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileApi: ApiService,
    private val sessionManager: SessionManager, // 用于管理 UI 状态 (如用户名/角色)
    private val authRepository: AuthRepository   // 用于管理核心 Auth 数据 (如 UserID/Token)
) : BaseViewModel() {

    var userInfo by mutableStateOf<UserInfoData?>(null)
        private set

    // === 编辑模式状态 ===
    var isEditing by mutableStateOf(false)
        private set

    // === 暂存编辑中的数据 ===
    var editName by mutableStateOf("")
    var editStudentId by mutableStateOf("")
    var editUsername by mutableStateOf("")
    var editRole by mutableStateOf("")
    var editDepartment by mutableStateOf("")
    var showPasswordDialog by mutableStateOf(false)
    var newPasswordInput by mutableStateOf("")

    fun fetchUserInfo() {
        launchDataLoad {
            //优先从 AuthRepository 获取 Token，因为它与 UserID 绑定更紧密
            val token = authRepository.getToken() ?: sessionManager.jwtToken

            if (token.isNullOrBlank()) {
                // 如果没有 Token，说明未登录或状态异常，执行登出清理
                logout()
                return@launchDataLoad
            }

            val response = profileApi.getUserInfo(token)

            if (response.isSuccessful && response.body()?.code == 200) {
                val data = response.body()?.data
                userInfo = data
                //获取最新信息后，顺便更新 SessionManager 的 UI 状态
                data?.let {
                    sessionManager.saveUserInfo(it.username, it.role)
                }
            } else {
                // Token 过期处理 (401)
                if (response.code() == 401) {
                    logout()
                    throw Exception("登录已过期")
                }
                throw Exception(response.body()?.message ?: "获取信息失败")
            }
        }
    }

    // 进入编辑模式
    fun startEditing() {
        userInfo?.let {
            editName = it.name
            editStudentId = it.student_id.toString()
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

    fun updatePassword(onSuccess: () -> Unit) {
        if (newPasswordInput.isBlank()) {
            errorMessage = "密码不能为空"
            return
        }

        launchDataLoad {
            val token = authRepository.getToken() ?: sessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdatePasswordRequest(oldPassword = "", newPassword = newPasswordInput)
            val response = profileApi.updatePassword(token, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                showPasswordDialog = false
                newPasswordInput = ""
                // 修改密码成功后，强制登出
                logout()
                onSuccess()
            } else {
                throw Exception(response.body()?.message ?: "修改密码失败")
            }
        }
    }

    fun saveUserInfo() {
        launchDataLoad {
            val token = authRepository.getToken() ?: sessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdateUserInfoRequest(
                student_id = editStudentId.toLongOrNull() ?: 0L,
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

    /**
     * 登出逻辑
     */
    fun logout() {
        //清除SessionManager (为了 UI 状态)
        sessionManager.clear()

        //清除AuthRepository (为了 UserID)
        authRepository.clearSession()

        //清空ViewModel状态
        userInfo = null
        isEditing = false
    }
}