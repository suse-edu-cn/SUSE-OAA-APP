package com.suseoaa.projectoaa.presentation.recruitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.repository.PersonRepository
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeStatusRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ChangeTimeRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val TIME_MANAGER_ROLES = setOf("副部长", "部长", "会长", "开发者")
private val REVIEW_ROLES = setOf("副部长", "部长", "会长", "开发者")

enum class RecruitmentFilterOption {
    FirstChoiceCurrentDepartment,
    SecondChoiceCurrentDepartment,
    All
}

data class RecruitmentUiState(
    val isLoading: Boolean = false,
    val isSubmissionTime: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val applications: List<RecruitmentApplication> = emptyList(),
    val filteredApplications: List<RecruitmentApplication> = emptyList(),
    val userRole: String = "",
    val userDepartment: String = "",
    val userStudentId: String = "",
    val canManageTime: Boolean = false,
    val canReviewApplications: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val activeFilter: RecruitmentFilterOption = RecruitmentFilterOption.FirstChoiceCurrentDepartment,
    val currentApplication: RecruitmentApplication = RecruitmentApplication(),
    val pickedAvatar: ByteArray? = null
)

class RecruitmentViewModel(
    private val repository: RecruitmentRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecruitmentUiState())
    val uiState: StateFlow<RecruitmentUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }

            personRepository.getPersonInfo().onSuccess { personInfo ->
                val role = personInfo.role.trim()
                _uiState.update {
                    it.copy(
                        userRole = role,
                        userDepartment = personInfo.department.orEmpty().trim(),
                        userStudentId = personInfo.studentId,
                        canManageTime = role in TIME_MANAGER_ROLES,
                        canReviewApplications = role in REVIEW_ROLES
                    )
                }
            }

            repository.getApplications().onSuccess { response ->
                val allApplications = response.data.orEmpty()
                val now = Clock.System.now()
                _uiState.update { state ->
                    val submissionTime = isTimeActive(response.starttime, response.endtime, now)
                    val ownApplication = findOwnApplication(allApplications, state.userStudentId)
                    val nextCurrent = if (submissionTime) {
                        ownApplication ?: RecruitmentApplication()
                    } else {
                        state.currentApplication
                    }
                    val nextFiltered = applyFilter(
                        applications = allApplications,
                        currentDepartment = state.userDepartment,
                        option = state.activeFilter
                    )

                    state.copy(
                        isLoading = false,
                        applications = allApplications,
                        filteredApplications = nextFiltered,
                        startTime = response.starttime.orEmpty(),
                        endTime = response.endtime.orEmpty(),
                        isSubmissionTime = submissionTime,
                        currentApplication = nextCurrent
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载申请表失败"
                    )
                }
            }
        }
    }

    fun updateFormField(updater: (RecruitmentApplication) -> RecruitmentApplication) {
        _uiState.update { state ->
            state.copy(currentApplication = updater(state.currentApplication))
        }
    }

    fun onAvatarPicked(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "未选择头像") }
            return
        }
        _uiState.update {
            it.copy(
                pickedAvatar = bytes,
                successMessage = "头像已选择，提交时会自动上传"
            )
        }
    }

    fun submitApplication() {
        val state = _uiState.value
        if (!state.isSubmissionTime) {
            _uiState.update { it.copy(errorMessage = "当前不在填写时间内，无法提交或修改") }
            return
        }

        val validationError = validateApplication(state.currentApplication)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val isUpdate = state.applications.isNotEmpty()
        if (!isUpdate && state.pickedAvatar == null && state.currentApplication.avatarUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先上传头像") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentForm = _uiState.value.currentApplication
            val pickedAvatar = _uiState.value.pickedAvatar
            var uploadedAvatarUrl: String? = null

            if (pickedAvatar != null) {
                val uploadResult = repository.uploadImage(
                    imageBytes = pickedAvatar,
                    filename = "recruitment-avatar.jpg"
                )
                if (uploadResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = uploadResult.exceptionOrNull()?.message ?: "头像上传失败"
                        )
                    }
                    return@launch
                }
                val raw = uploadResult.getOrNull().orEmpty()
                if (raw.startsWith("http")) {
                    uploadedAvatarUrl = raw
                }
            }

            val formWithUploadedAvatar = if (uploadedAvatarUrl.isNullOrBlank()) {
                currentForm
            } else {
                currentForm.copy(avator = uploadedAvatarUrl, avatar = uploadedAvatarUrl)
            }

            if (isUpdate) {
                repository.updateApplication(formWithUploadedAvatar)
                    .onSuccess { response ->
                        handleUpdateResponse(response, formWithUploadedAvatar)
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentApplication = formWithUploadedAvatar,
                                errorMessage = error.message ?: "更新失败"
                            )
                        }
                    }
            } else {
                repository.createApplication(formWithUploadedAvatar)
                    .onSuccess { response ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pickedAvatar = null,
                                currentApplication = formWithUploadedAvatar,
                                startTime = response.starttime ?: it.startTime,
                                endTime = response.endtime ?: it.endTime,
                                successMessage = response.message.ifBlank { "提交成功" }
                            )
                        }
                        loadInitialData(showLoading = false)
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "提交失败"
                            )
                        }
                    }
            }
        }
    }

    private fun handleUpdateResponse(
        response: RecruitmentResponse<RecruitmentApplication>,
        originalApplication: RecruitmentApplication
    ) {
        val responseData = response.data
        if (responseData == null || isUpdateBlockedResponse(responseData)) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentApplication = originalApplication,
                    errorMessage = response.message.ifBlank { "更新失败" }
                )
            }
            return
        }

        val merged = mergeApplication(originalApplication, responseData)
        _uiState.update {
            it.copy(
                isLoading = false,
                currentApplication = merged,
                pickedAvatar = null,
                successMessage = response.message.ifBlank { "更新成功" }
            )
        }
        loadInitialData(showLoading = false)
    }

    fun updateTime(start: String, end: String) {
        if (!_uiState.value.canManageTime) {
            _uiState.update { it.copy(errorMessage = "仅副部长、部长、会长、开发者可修改填写时间") }
            return
        }
        if (start.isBlank() || end.isBlank()) {
            _uiState.update { it.copy(errorMessage = "开始时间和结束时间不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.updateTime(ChangeTimeRequest(starttime = start, endtime = end)).onSuccess { message ->
                val submissionTime = isTimeActive(start, end, Clock.System.now())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = message,
                        startTime = start,
                        endTime = end,
                        isSubmissionTime = submissionTime
                    )
                }
                loadInitialData(showLoading = false)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "修改时间失败"
                    )
                }
            }
        }
    }

    fun changeStatus(studentIds: List<String>, statusOptions: List<String>) {
        if (_uiState.value.isSubmissionTime) {
            _uiState.update { it.copy(errorMessage = "填写时间内不可进行录取操作") }
            return
        }
        if (!_uiState.value.canReviewApplications) {
            _uiState.update { it.copy(errorMessage = "当前账号无录取权限") }
            return
        }
        if (studentIds.isEmpty() || statusOptions.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "学号和录取结果不能为空") }
            return
        }
        if (studentIds.size != statusOptions.size) {
            _uiState.update { it.copy(errorMessage = "学号和录取结果数量必须一致") }
            return
        }
        val invalidStatus = statusOptions.firstOrNull { !isValidStatusFormat(it) }
        if (invalidStatus != null) {
            _uiState.update {
                it.copy(errorMessage = "录取状态格式错误：$invalidStatus。调剂必须为：调剂到xxx部门xxx职位")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // studentIds[i] 与 statusOptions[i] 按索引一一绑定，不做重排。
            repository.changeStatus(ChangeStatusRequest(studentid = studentIds, status = statusOptions))
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = message.ifBlank { "更新成功" }
                        )
                    }
                    loadInitialData(showLoading = false)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "更新状态失败"
                        )
                    }
                }
        }
    }

    fun changeSingleStatus(application: RecruitmentApplication, status: String) {
        val studentId = application.resolvedStudentId
        if (studentId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "该申请缺少学号，无法更新状态") }
            return
        }
        changeStatus(studentIds = listOf(studentId), statusOptions = listOf(status))
    }

    fun changeStatusInOrder(entries: List<Pair<String, String>>) {
        if (entries.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "待更新状态列表不能为空") }
            return
        }
        changeStatus(
            studentIds = entries.map { it.first },
            statusOptions = entries.map { it.second }
        )
    }

    fun setFilterOption(option: RecruitmentFilterOption) {
        _uiState.update { state ->
            state.copy(
                activeFilter = option,
                filteredApplications = applyFilter(state.applications, state.userDepartment, option)
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun validateApplication(application: RecruitmentApplication): String? {
        val requiredFields = listOf(
            "申请理由" to application.reason,
            "第一志愿" to application.choice1,
            "第二志愿" to application.choice2,
            "工作经历" to application.experience,
            "手机号" to application.phone,
            "性别" to application.gender,
            "专业" to application.major,
            "班级" to application.className,
            "生日" to application.birthday,
            "QQ" to application.qq,
            "政治面貌" to application.politic_stance,
            "申请职位一" to application.role1,
            "申请职位二" to application.role2
        )
        return requiredFields.firstOrNull { it.second.isBlank() }?.let { "${it.first}不能为空" }
    }

    private fun isUpdateBlockedResponse(application: RecruitmentApplication): Boolean {
        val keyFields = listOf(
            application.reason,
            application.choice1,
            application.choice2,
            application.experience,
            application.phone,
            application.gender,
            application.major,
            application.className,
            application.birthday,
            application.qq,
            application.politic_stance,
            application.role1,
            application.role2
        )
        return keyFields.all { it.isBlank() }
    }

    private fun mergeApplication(
        old: RecruitmentApplication,
        remote: RecruitmentApplication
    ): RecruitmentApplication {
        return old.copy(
            id = if (remote.id != 0) remote.id else old.id,
            name = remote.name.ifBlank { old.name },
            reason = remote.reason.ifBlank { old.reason },
            choice1 = remote.choice1.ifBlank { old.choice1 },
            choice2 = remote.choice2.ifBlank { old.choice2 },
            experience = remote.experience.ifBlank { old.experience },
            phone = remote.phone.ifBlank { old.phone },
            gender = remote.gender.ifBlank { old.gender },
            major = remote.major.ifBlank { old.major },
            className = remote.className.ifBlank { old.className },
            birthday = remote.birthday.ifBlank { old.birthday },
            qq = remote.qq.ifBlank { old.qq },
            politic_stance = remote.politic_stance.ifBlank { old.politic_stance },
            adjustment = remote.adjustment,
            studentId = remote.studentId.ifBlank { old.studentId },
            studentIdCompat = remote.studentIdCompat.ifBlank { old.studentIdCompat },
            avator = remote.avator.ifBlank { old.avator },
            avatar = remote.avatar.ifBlank { old.avatar },
            createdAt = remote.createdAt.ifBlank { old.createdAt },
            status = remote.status.ifBlank { old.status },
            role1 = remote.role1.ifBlank { old.role1 },
            role2 = remote.role2.ifBlank { old.role2 }
        )
    }

    private fun findOwnApplication(
        applications: List<RecruitmentApplication>,
        studentId: String
    ): RecruitmentApplication? {
        if (applications.isEmpty()) return null
        if (studentId.isBlank()) return applications.firstOrNull()
        return applications.firstOrNull { it.resolvedStudentId == studentId } ?: applications.firstOrNull()
    }

    private fun applyFilter(
        applications: List<RecruitmentApplication>,
        currentDepartment: String,
        option: RecruitmentFilterOption
    ): List<RecruitmentApplication> {
        return when (option) {
            RecruitmentFilterOption.All -> applications
            RecruitmentFilterOption.FirstChoiceCurrentDepartment -> {
                if (currentDepartment.isBlank()) applications else applications.filter { it.choice1 == currentDepartment }
            }
            RecruitmentFilterOption.SecondChoiceCurrentDepartment -> {
                if (currentDepartment.isBlank()) applications else applications.filter { it.choice2 == currentDepartment }
            }
        }
    }

    private fun isTimeActive(startStr: String?, endStr: String?, now: Instant): Boolean {
        val start = parseBackendDateTime(startStr)
        val end = parseBackendDateTime(endStr)
        if (start == null || end == null) return true
        return now >= start && now <= end
    }

    private fun parseBackendDateTime(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            LocalDateTime.parse(raw.trim().replace(" ", "T"))
                .toInstant(TimeZone.currentSystemDefault())
        }.getOrNull()
    }

    private fun isValidStatusFormat(status: String): Boolean {
        val normalized = status.trim()
        if (normalized.isBlank()) return false
        if (normalized == "录取第1志愿" || normalized == "录取第2志愿" || normalized == "未通过") {
            return true
        }
        return "^调剂到.+部门.+职位$".toRegex().matches(normalized)
    }
}
