package com.suseoaa.projectoaa.competition.viewmodel

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.competition.model.CreateMatchRequest
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateMatchViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    // 表单状态
    var title by mutableStateOf("")
    var description by mutableStateOf("") // 普通文本
    var content by mutableStateOf("")     // 需要转 Base64

    // 日期状态
    // UI 组件现在直接返回标准格式: "yyyy-MM-dd HH:mm:ss"
    var regStartTime by mutableStateOf("")
    var regEndTime by mutableStateOf("")
    var matchStartTime by mutableStateOf("")
    var matchEndTime by mutableStateOf("")

    var isSubmitSuccess by mutableStateOf(false)
        private set

    fun createMatch() {
        if (!validateInputs()) {
            errorMessage = "请完整填写所有信息"
            return
        }

        val token = sessionManager.jwtToken
        if (token.isNullOrBlank()) {
            errorMessage = "未登录"
            return
        }

        launchDataLoad {
            // 1. Content 转 Base64 (对应后端要求)
            // NO_WRAP 避免产生换行符
            val encodedContent = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            // 2. 构造请求对象
            val request = CreateMatchRequest(
                title = title,
                content = encodedContent,
                description = description,
                // 3. 直接使用 UI 传来的完整时间字符串，无需额外处理
                startAt = matchStartTime,
                endAt = matchEndTime,
                regStartAt = regStartTime,
                regEndAt = regEndTime
            )

            val success = repository.createMatch(token, request)
            if (success) {
                isSubmitSuccess = true
            }
        }
    }

    private fun validateInputs(): Boolean {
        return title.isNotBlank() && description.isNotBlank() && content.isNotBlank() &&
                regStartTime.isNotBlank() && regEndTime.isNotBlank() &&
                matchStartTime.isNotBlank() && matchEndTime.isNotBlank()
    }

    fun resetState() {
        isSubmitSuccess = false
        errorMessage = null
    }
}