package com.suseoaa.projectoaa.competition.viewmodel

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
    private val sessionManager: SessionManager //inject: javax.inject.Inject
) : BaseViewModel() {

    // 表单状态
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var content by mutableStateOf("")

    // 日期状态 (yyyy-MM-dd)
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
            errorMessage = "未登录，无法发布比赛"
            return
        }

        launchDataLoad {
            val request = CreateMatchRequest(
                title = title,
                content = content,
                description = description,
                startAt = matchStartTime,
                endAt = matchEndTime,
                regStartAt = regStartTime,
                regEndAt = regEndTime
            )

            //token
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