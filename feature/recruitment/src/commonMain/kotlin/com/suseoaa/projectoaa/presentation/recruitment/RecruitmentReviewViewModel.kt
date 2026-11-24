package com.suseoaa.projectoaa.presentation.recruitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldsConfig
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportedSpreadsheet
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecision
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecisionRules
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewResult
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.pickCurrent
import com.suseoaa.projectoaa.shared.domain.repository.OrganizationRepository
import com.suseoaa.projectoaa.shared.domain.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class RecruitmentReviewUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val terms: List<Term> = emptyList(),
    val selectedTerm: Term? = null,
    val departments: List<Department> = emptyList(),
    val roles: List<Role> = emptyList(),
    /** null 表示不指定部门，由后端返回面试官所属部门的申请 */
    val departmentFilter: Department? = null,
    /** null 表示全部，false 未评审，true 已评审 */
    val hasResultFilter: Boolean? = null,
    val nameFilter: String = "",
    val applications: List<Application> = emptyList(),
    /** 申请 ID -> 已录入的面试结果 */
    val results: Map<Int, InterviewResult> = emptyMap(),
    val decisions: List<String> = emptyList(),
    val exportFieldsConfig: ExportFieldsConfig? = null,
    val isLoadingExportFields: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFile: ExportedSpreadsheet? = null
) {
    fun departmentName(id: Int): String = departments.firstOrNull { it.id == id }?.name ?: "未知部门"
    fun roleName(id: Int): String = roles.firstOrNull { it.id == id }?.name ?: "未知职位"
}

/**
 * 招新换届 —— 面试官视角：查看本周期的申请，录入 / 修改面试结论。
 */
class RecruitmentReviewViewModel(
    private val recruitmentRepository: RecruitmentRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecruitmentReviewUiState())
    val uiState: StateFlow<RecruitmentReviewUiState> = _uiState.asStateFlow()

    private var initialized = false

    /** 审核页是按需打开的 Tab，第一次切过来时才加载。 */
    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val departments = organizationRepository.getDepartments().getOrDefault(emptyList())
            val roles = organizationRepository.getRoles().getOrDefault(emptyList())
            val decisions = recruitmentRepository.getDecisions().getOrDefault(emptyList())
            val terms = recruitmentRepository.getTerms().getOrElse { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: "获取招新周期失败") }
                return@launch
            }
            val today = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            _uiState.update {
                it.copy(
                    departments = departments,
                    roles = roles,
                    decisions = decisions,
                    terms = terms,
                    selectedTerm = terms.pickCurrent(today)
                )
            }
            reload()
        }
    }

    fun selectTerm(term: Term) {
        _uiState.update { it.copy(selectedTerm = term) }
        reload()
    }

    fun selectDepartment(department: Department?) {
        _uiState.update { it.copy(departmentFilter = department) }
        reload()
    }

    fun selectHasResult(hasResult: Boolean?) {
        _uiState.update { it.copy(hasResultFilter = hasResult) }
        reload()
    }

    fun updateNameFilter(name: String) {
        _uiState.update { it.copy(nameFilter = name) }
    }

    fun searchByName() {
        reload()
    }

    fun loadExportFields() {
        if (_uiState.value.isLoadingExportFields || _uiState.value.exportFieldsConfig != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExportFields = true) }
            recruitmentRepository.getExportFields()
                .onSuccess { config ->
                    _uiState.update { it.copy(isLoadingExportFields = false, exportFieldsConfig = config) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingExportFields = false,
                            message = error.message ?: "获取导出字段失败"
                        )
                    }
                }
        }
    }

    fun exportResults(selectedIndices: Set<Int>) {
        if (_uiState.value.isExporting) return
        val term = _uiState.value.selectedTerm ?: run {
            _uiState.update { it.copy(message = "请先选择招新周期") }
            return
        }
        if (selectedIndices.isEmpty()) {
            _uiState.update { it.copy(message = "请至少选择一个导出字段") }
            return
        }
        val fieldsBits = _uiState.value.exportFieldsConfig?.buildBits(selectedIndices)
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            recruitmentRepository.exportInterviewResults(
                termId = term.resolvedId,
                departmentId = _uiState.value.departmentFilter?.id,
                hasResult = _uiState.value.hasResultFilter,
                fieldsBits = fieldsBits
            ).onSuccess { file ->
                _uiState.update { it.copy(isExporting = false, exportedFile = file) }
            }.onFailure { error ->
                _uiState.update { it.copy(isExporting = false, message = error.message ?: "导出面试结果失败") }
            }
        }
    }

    fun consumeExportedFile() {
        _uiState.update { it.copy(exportedFile = null) }
    }

    fun reportMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun reload() {
        val term = _uiState.value.selectedTerm ?: run {
            _uiState.update { it.copy(isLoading = false, applications = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val termId = term.resolvedId
            val state = _uiState.value
            val applicationsDeferred = async {
                recruitmentRepository.getApplications(
                    termId = termId,
                    departmentId = state.departmentFilter?.id,
                    hasResult = state.hasResultFilter,
                    name = state.nameFilter
                )
            }
            val resultsDeferred = async { recruitmentRepository.getInterviewResults(termId) }
            val applications = applicationsDeferred.await()
            val results = resultsDeferred.await().getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    applications = applications.getOrDefault(emptyList()),
                    results = results.filter { r -> r.applicationId != 0 }.associateBy { r -> r.applicationId },
                    message = applications.exceptionOrNull()?.message
                )
            }
        }
    }

    fun saveDecision(application: Application, decision: InterviewDecision) {
        if (_uiState.value.isSaving) return
        if (decision.decision.isBlank()) {
            _uiState.update { it.copy(message = "请选择面试结论") }
            return
        }
        if (application.resolvedId == 0) {
            _uiState.update { it.copy(message = "后端未返回申请编号，无法录入结论") }
            return
        }
        val currentApplication = _uiState.value.applications.firstOrNull { it.resolvedId == application.resolvedId } ?: application
        if (!InterviewDecisionRules.isAllowed(currentApplication.isGraduated, decision.decision)) {
            _uiState.update { it.copy(message = "该申请人已毕业，仅可标记为淘汰") }
            return
        }
        val normalized = if (InterviewDecisionRules.isNonAdmission(decision.decision)) {
            decision.copy(resultDepartmentId = 0, resultRoleId = 0)
        } else decision
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val existing = _uiState.value.results[application.resolvedId]
            recruitmentRepository.saveInterviewResult(
                applicationId = application.resolvedId,
                existingResultId = existing?.resolvedId,
                decision = normalized
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "已保存面试结论") }
                reload()
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, message = error.message ?: "保存失败") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
