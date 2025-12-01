package com.suseoaa.projectoaa.student.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
// [解耦] 1. 导入你的服务类
import com.suseoaa.projectoaa.common.util.ImageCompressor
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.student.model.FormFieldErrors
import com.suseoaa.projectoaa.student.model.StudentApplicationData
import com.suseoaa.projectoaa.student.network.ApplicationRequest
import com.suseoaa.projectoaa.student.repository.StudentRepository
import com.suseoaa.projectoaa.student.util.FormValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// [解耦] 2. 移除 java.io.File 导入，ViewModel 不再关心文件系统

// (StudentFormEvent 保持不变)
sealed class StudentFormEvent {
    object SubmissionSuccess : StudentFormEvent()
    data class SubmissionError(val message: String) : StudentFormEvent()
    data class ImageError(val message: String) : StudentFormEvent()
    data class AuthError(val message: String) : StudentFormEvent()
}

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    // [解耦] 3. 注入服务类
    private val repository: StudentRepository,
    private val sessionManager: SessionManager,
    private val validator: FormValidator,       // <-- 新增
    private val compressor: ImageCompressor     // <-- 新增
    // [解耦] 4. 移除 @ApplicationContext private val context: Context
) : ViewModel() {

    // (UI 状态, 错误状态, 加载状态, 事件流都保持不变)
    var formData by mutableStateOf(StudentApplicationData())
        private set
    var formErrors by mutableStateOf(FormFieldErrors())
        private set
    var isLoading by mutableStateOf(false)
        private set
    private val _eventFlow = MutableSharedFlow<StudentFormEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // (所有 update... 函数保持不变)
    fun initType(type: String) {}
    fun updateName(v: String) {
        formData = formData.copy(name = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(name = null)
    }
    fun updateGender(v: String) { formData = formData.copy(gender = v) }
    fun updateCollege(v: String) {
        formData = formData.copy(college = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(college = null)
    }
    fun updateMajorClass(v: String) {
        formData = formData.copy(majorClass = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(majorClass = null)
    }
    fun updatePoliticalStatus(v: String) {
        formData = formData.copy(politicalStatus = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(politicalStatus = null)
    }
    fun updateBirthDate(v: String) {
        formData = formData.copy(birthDate = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(birthDate = null)
    }
    fun updatePhone(v: String) {
        formData = formData.copy(phoneNumber = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(phoneNumber = null)
    }
    fun updateQQ(v: String) {
        formData = formData.copy(qq = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(qq = null)
    }
    fun updatePhoto(uri: Uri?) {
        formData = formData.copy(photoUri = uri)
        if (uri != null) formErrors = formErrors.copy(photoError = null)
    }
    fun updateResume(v: String) {
        formData = formData.copy(resume = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(resume = null)
    }
    fun updateReason(v: String) {
        formData = formData.copy(reason = v)
        if (v.isNotBlank()) formErrors = formErrors.copy(reason = null)
    }
    fun updateFirstChoice(choice: String) {
        formData = formData.copy(firstChoice = choice)
        if (choice.isNotBlank()) formErrors = formErrors.copy(firstChoice = null)
    }
    fun updateSecondChoice(choice: String) {
        formData = formData.copy(secondChoice = choice)
    }
    fun updateObeyAdjustment(v: Boolean) {
        formData = formData.copy(isObeyAdjustment = v)
    }

    // --- 核心提交逻辑 ---

    fun submitForm() {
        var compressedPhotoUri: Uri? = null

        viewModelScope.launch {
            isLoading = true
            try {
                // 1. 校验
                // [解耦] 5. 使用注入的 validator，不再传递 context
                val errors = validator.validateInput(formData)
                if (errors.hasErrors()) {
                    formErrors = errors
                    return@launch
                }
                formErrors = FormFieldErrors()

                // 2. 处理图片 (IO操作)
                if (formData.photoUri != null) {
                    // [解耦] 6. 使用注入的 compressor，不再传递 context
                    val resultUri = withContext(Dispatchers.IO) {
                        compressor.compressImage(formData.photoUri)
                    }
                    compressedPhotoUri = resultUri

                    if (resultUri == null) {
                        _eventFlow.emit(StudentFormEvent.ImageError("图片处理失败，请重试"))
                        return@launch
                    }
                }

                // 3. 构建 DTO (不变)
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

                // 4. 检查Token (不变)
                val token = sessionManager.jwtToken
                if (token.isNullOrBlank()) {
                    _eventFlow.emit(StudentFormEvent.AuthError("错误：未登录或Token已过期"))
                    return@launch
                }

                // 5. 调用仓库
                val result = repository.submitApplication(token, apiRequest)

                // 6. 处理结果
                result.onSuccess {
                    _eventFlow.emit(StudentFormEvent.SubmissionSuccess)
                }.onFailure { e ->
                    _eventFlow.emit(StudentFormEvent.SubmissionError("提交失败: ${e.message}"))
                }

            } catch (e: Exception) {
                _eventFlow.emit(StudentFormEvent.SubmissionError("发生未知错误: ${e.message}"))
            } finally {
                isLoading = false
                // [解耦] 7. 委托 compressor 清理文件，ViewModel 不再关心 File API
                compressedPhotoUri?.let {
                    compressor.cleanupCompressedImage(it)
                }
            }
        }
    }
}