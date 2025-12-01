package com.suseoaa.projectoaa.student.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.util.ImageCompressor
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.student.model.FormFieldErrors
import com.suseoaa.projectoaa.student.model.StudentApplicationData
import com.suseoaa.projectoaa.student.network.ApplicationRequest
import com.suseoaa.projectoaa.student.repository.StudentRepository
import com.suseoaa.projectoaa.student.util.FormValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StudentFormViewModel : BaseViewModel() {

    // --- UI 数据状态 ---
    var formData by mutableStateOf(StudentApplicationData())
        private set

    // --- 表单校验错误信息 ---
    var formErrors by mutableStateOf(FormFieldErrors())
        private set

    // 引入仓库
    private val repository = StudentRepository()

    // --- 初始化与字段更新方法 (保持不变) ---

    fun initType(type: String) {
        formData = StudentApplicationData(type = type)
        formErrors = FormFieldErrors()
    }

    fun updateName(v: String) {
        formData = formData.copy(name = v)
        formErrors = formErrors.copy(name = null)
    }

    fun updateGender(v: String) {
        formData = formData.copy(gender = v)
    }

    fun updateCollege(v: String) {
        formData = formData.copy(college = v)
        formErrors = formErrors.copy(college = null)
    }

    fun updateMajorClass(v: String) {
        formData = formData.copy(majorClass = v)
        formErrors = formErrors.copy(majorClass = null)
    }

    fun updatePoliticalStatus(v: String) {
        formData = formData.copy(politicalStatus = v)
        formErrors = formErrors.copy(politicalStatus = null)
    }

    fun updateBirthDate(v: String) {
        formData = formData.copy(birthDate = v)
        formErrors = formErrors.copy(birthDate = null)
    }

    fun updatePhone(v: String) {
        formData = formData.copy(phoneNumber = v)
        formErrors = formErrors.copy(phoneNumber = null)
    }

    fun updateQQ(v: String) {
        formData = formData.copy(qq = v)
        formErrors = formErrors.copy(qq = null)
    }

    fun updatePhoto(uri: Uri?) {
        formData = formData.copy(photoUri = uri)
        formErrors = formErrors.copy(photoError = null)
    }

    fun updateResume(v: String) {
        formData = formData.copy(resume = v)
        formErrors = formErrors.copy(resume = null)
    }

    fun updateReason(v: String) {
        formData = formData.copy(reason = v)
        formErrors = formErrors.copy(reason = null)
    }

    fun updateFirstChoice(choice: String) {
        formData = formData.copy(firstChoice = choice)
        // 互斥逻辑：如果第一志愿和第二志愿相同，清空第二志愿
        if (formData.secondChoice == choice) {
            formData = formData.copy(secondChoice = "")
        }
        formErrors = formErrors.copy(firstChoice = null)
    }

    fun updateSecondChoice(choice: String) {
        formData = formData.copy(secondChoice = choice)
    }

    fun updateObeyAdjustment(v: Boolean) {
        formData = formData.copy(isObeyAdjustment = v)
    }

    // --- 核心提交逻辑 ---

    fun submitForm(context: Context, onSuccess: () -> Unit) {
        // 临时变量，用于 finally 块清理图片缓存
        var compressedPhotoUri: Uri? = null

        // 使用基类 launchDataLoad 自动管理 isLoading
        launchDataLoad {
            // 1. 本地校验
            val errors = FormValidator.validateInput(formData, context)
            if (errors.hasErrors()) {
                formErrors = errors
                return@launchDataLoad // 校验失败，直接返回，不触发 loading
            }
            // 清除旧错误
            formErrors = FormFieldErrors()

            try {
                // 2. 图片处理 (耗时操作)
                if (formData.photoUri != null) {
                    val resultUri = withContext(Dispatchers.IO) {
                        ImageCompressor.compressImage(context, formData.photoUri)
                    }
                    compressedPhotoUri = resultUri

                    if (resultUri == null) {
                        Toast.makeText(context, "图片处理失败，请重试", Toast.LENGTH_LONG).show()
                        return@launchDataLoad
                    }
                }

                // 3. 构建 DTO 对象
                val apiRequest = ApplicationRequest(
                    name = formData.name,
                    reason = formData.reason,
                    choice1 = formData.firstChoice,
                    choice2 = if (formData.secondChoice.isBlank()) "无" else formData.secondChoice,
                    experience = formData.resume,
                    phone = formData.phoneNumber,
                    gender = formData.gender,
                    major = formData.college,
                    className = formData.majorClass,
                    birthday = formData.birthDate,
                    qq = formData.qq,
                    politicStance = formData.politicalStatus,
                    adjustiment = if (formData.isObeyAdjustment) 1 else 0
                )

                // 4. 获取 Token
                val token = SessionManager.jwtToken
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "错误：未登录或Token已过期", Toast.LENGTH_SHORT).show()
                    return@launchDataLoad
                }

                // 5. 调用仓库提交 (网络请求)
                val result = repository.submitApplication(token, apiRequest)

                // 6. 处理结果
                result.onSuccess {
                    Toast.makeText(context, "提交成功", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }.onFailure { e ->
                    Toast.makeText(context, "提交失败: ${e.message}", Toast.LENGTH_LONG).show()
                }

            } finally {
                // 7. 无论成功失败，清理压缩产生的临时文件
                compressedPhotoUri?.path?.let {
                    val file = File(it)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }
}