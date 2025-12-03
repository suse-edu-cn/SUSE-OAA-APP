package com.suseoaa.projectoaa.login.viewmodel

// [修复] 移除了 Context 导入
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
// [修复] 导入 Hilt 和 Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
// [修复] 移除了 NetworkModule 导入
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.login.api.ApiService
import com.suseoaa.projectoaa.login.model.UpdatePasswordRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import com.suseoaa.projectoaa.login.model.UpdateUserInfoRequest

// [修复] 1. 添加 Hilt 注解
@HiltViewModel
class ProfileViewModel @Inject constructor(
    // [修复] 2. 注入 ApiService 和 SessionManager
    private val profileApi: ApiService,
    private val sessionManager: SessionManager
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

    // [修复] 3. 移除了 profileApi 的 lazy 创建

    fun fetchUserInfo() {
        launchDataLoad {
            // [修复] 4. 使用注入的 sessionManager
            val token = sessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效，请重新登录")

            // [修复] 5. 使用注入的 profileApi
            val response = profileApi.getUserInfo(token)

            if (response.isSuccessful && response.body()?.code == 200) {
                userInfo = response.body()?.data
            } else {
                throw Exception(response.body()?.message ?: "获取信息失败")
            }
        }
    }

    // 进入编辑模式 (不变)
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

    // 取消编辑 (不变)
    fun cancelEditing() {
        isEditing = false
    }

    // [修复] 6. 移除了 context 参数
    fun updatePassword(onSuccess:()-> Unit) {
        if (newPasswordInput.isBlank()) {
            errorMessage = "密码不能为空"
            return
        }

        launchDataLoad {
            // [修复] 7. 使用注入的 sessionManager
            val token = sessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdatePasswordRequest(oldPassword = "", newPassword = newPasswordInput)

            // [修复] 8. 使用注入的 profileApi
            val response = profileApi.updatePassword(token, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                showPasswordDialog = false
                newPasswordInput = ""
                logout() // [修复] 9. 调用本地的 logout (无 context)
                onSuccess()
            } else {
                throw Exception(response.body()?.message ?: "修改密码失败")
            }
        }
    }

    // 提交保存
    fun saveUserInfo() {
        launchDataLoad {
            // [修复] 10. 使用注入的 sessionManager
            val token = sessionManager.jwtToken
            if (token.isNullOrBlank()) throw IllegalStateException("Token失效")

            val request = UpdateUserInfoRequest(
                studentid = editStudentId.toLongOrNull() ?: 0L,
                name = editName,
                role = editRole,
                department = editDepartment
            )

            // [修复] 11. 使用注入的 profileApi
            val response = profileApi.updateUserInfo(token, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                isEditing = false
                fetchUserInfo()
            } else {
                throw Exception(response.body()?.message ?: "修改失败")
            }
        }
    }

    // [修复] 12. 移除了 context 参数
    fun logout() {
        // 1. 清除本地所有 Token 和数据
        sessionManager.clear() // [修复] 13. 使用注入的 sessionManager
        // 2. 清空当前 ViewModel 的数据
        userInfo = null
    }
}