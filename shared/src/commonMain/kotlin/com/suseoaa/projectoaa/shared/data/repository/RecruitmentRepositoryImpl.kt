package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.remote.api.decodeListLenient
import com.suseoaa.projectoaa.shared.data.remote.api.toStringListLenient
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.person.OssObjectKey
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateApplicationRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateInterviewResultRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateInterviewersRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateTermRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldsConfig
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportedSpreadsheet
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecision
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewResult
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Interviewer
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewerEntry
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewerIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Period
import com.suseoaa.projectoaa.shared.domain.model.recruitment.SCENE_APPLICATION_IMAGE
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateApplicationRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateInterviewResultRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateInterviewerRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateTermRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.toRequest
import com.suseoaa.projectoaa.shared.domain.repository.RecruitmentRepository
import com.suseoaa.projectoaa.shared.util.AppLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * 招新换届仓库：周期、申请、面试。
 */
class RecruitmentRepositoryImpl(
    private val api: OaaRecruitmentApi,
    private val json: Json,
    private val userApi: OaaUserApi? = null
) : RecruitmentRepository {

    // ==================== 周期 ====================

    override suspend fun getTerms(year: Int?, type: String?): Result<List<Term>> =
        api.terms(year = year, type = type?.ifBlank { null }).map { it.orEmpty() }

    override suspend fun createTerm(
        year: Int,
        type: String,
        title: String,
        editPeriod: Period,
        queryPeriod: Period
    ): Result<Unit> = api.createTerm(
        CreateTermRequest(
            year = year,
            type = type,
            title = title,
            editPeriod = editPeriod.toRequest(),
            queryPeriod = queryPeriod.toRequest()
        )
    )

    override suspend fun updateTerm(
        termId: Int,
        title: String,
        editPeriod: Period,
        queryPeriod: Period
    ): Result<Unit> = api.updateTerm(
        UpdateTermRequest(
            termId = termId,
            title = title,
            editPeriod = editPeriod.toRequest(),
            queryPeriod = queryPeriod.toRequest()
        )
    )

    override suspend fun deleteTerm(termId: Int): Result<Unit> =
        api.deleteTerm(TermIdRequest(termId))

    // ==================== 申请 ====================

    override suspend fun uploadApplicationAvatar(imageData: ByteArray): Result<StoredFile> {
        val uploader = userApi
            ?: return appFailure(AppError.Business(userMessage = "图片上传服务未初始化"))
        return uploader.uploadImage(
            scene = SCENE_APPLICATION_IMAGE,
            bytes = imageData,
            fileName = "application.jpg",
            mimeType = "image/jpeg"
        )
    }

    override suspend fun getMyApplications(): Result<List<Application>> =
        api.myApplications().decodeList("获取我的申请失败")

    override suspend fun createApplication(termId: Int, form: ApplicationForm): Result<Unit> {
        val avatarKey = OssObjectKey.parse(form.avatarUri)
            ?: return appFailure(AppError.Business(userMessage = "申请表照片信息无效，请重新上传"))
        AppLog.d("Create application: termId=$termId, avatar=$avatarKey", tag = "OaaPhoto")
        return api.createApplication(
            CreateApplicationRequest(
                termId = termId,
                college = form.college,
                majorClass = form.majorClass,
                gender = form.gender,
                avatar = avatarKey,
                phone = form.phone,
                qq = form.qq,
                politicalStatus = form.politicalStatus,
                birthDate = form.birthDate,
                firstChoice = form.firstChoice.toRequest(),
                secondChoice = form.secondChoice.toRequest(),
                allowAdjust = form.allowAdjust,
                resume = form.resume,
                reason = form.reason
            )
        ).onFailure { AppLog.d("Create application failed: ${it.message}", tag = "OaaPhoto") }
    }

    override suspend fun updateApplication(termId: Int, existing: Application, form: ApplicationForm): Result<Unit> {
        val avatarKey = OssObjectKey.parse(form.avatarUri.ifBlank { existing.avatar.uri })
            ?: return appFailure(AppError.Business(userMessage = "申请表照片信息无效，请重新上传"))
        AppLog.d("Update application: termId=$termId, avatar=$avatarKey", tag = "OaaPhoto")
        return api.updateApplication(
            UpdateApplicationRequest(
                applicationId = existing.resolvedId.takeIf { it != 0 },
                termId = termId,
                college = form.college,
                majorClass = form.majorClass,
                gender = form.gender,
                avatar = avatarKey,
                phone = form.phone,
                qq = form.qq,
                politicalStatus = form.politicalStatus,
                birthDate = form.birthDate,
                firstChoice = form.firstChoice.toRequest(),
                secondChoice = form.secondChoice.toRequest(),
                allowAdjust = form.allowAdjust,
                resume = form.resume,
                reason = form.reason
            )
        ).onFailure { AppLog.d("Update application failed: ${it.message}", tag = "OaaPhoto") }
    }

    override suspend fun deleteApplication(applicationId: Int): Result<Unit> =
        api.deleteApplication(ApplicationIdRequest(applicationId))

    override suspend fun getApplications(
        termId: Int,
        departmentId: Int?,
        hasResult: Boolean?,
        name: String?
    ): Result<List<Application>> {
        val applications = api.applications(
            termId = termId,
            departmentId = departmentId,
            hasResult = hasResult,
            name = name
        ).decodeList<Application>("获取申请列表失败").getOrElse { return Result.failure(it) }
        if (applications.none { it.isGraduated == null }) return Result.success(applications)

        // 申请列表的毕业字段尚未写入所有版本的接口文档，优先使用列表返回值，缺失时关联成员信息。
        // 查询无权限或失败时保留未知状态，由后端在保存决定时继续校验。
        val members = userApi?.list(UserQuery(isAll = true))?.getOrNull()?.list
            ?: return Result.success(applications)
        val byId = members.filter { it.userId > 0 }.associateBy { it.userId }
        val byStudentId = members.filter { it.studentId.isNotBlank() }.associateBy { it.studentId }
        return Result.success(applications.map { application ->
            val member = if (application.userId > 0) byId[application.userId] else byStudentId[application.studentId]
            if (application.isGraduated == null) application.copy(isGraduated = member?.isGraduated) else application
        })
    }

    override suspend fun getFillableRoles(departmentId: Int): Result<List<Role>> =
        api.fillableRoles(departmentId).decodeList("获取可申请职位失败")

    override suspend fun getFillableDepartments(roleId: Int): Result<List<Department>> =
        api.fillableDepartments(roleId).decodeList("获取可申请部门失败")

    // ==================== 面试 ====================

    override suspend fun getInterviewers(termId: Int): Result<List<Interviewer>> =
        api.interviewers(termId).decodeList("获取面试官列表失败")

    override suspend fun createInterviewers(termId: Int, interviewers: List<InterviewerEntry>): Result<Unit> =
        api.createInterviewers(CreateInterviewersRequest(termId = termId, interviewers = interviewers))

    override suspend fun updateInterviewer(interviewerId: Int, remark: String): Result<Unit> =
        api.updateInterviewer(UpdateInterviewerRequest(interviewerId = interviewerId, remark = remark))

    override suspend fun deleteInterviewer(interviewerId: Int): Result<Unit> =
        api.deleteInterviewer(InterviewerIdRequest(interviewerId))

    override suspend fun getDecisions(): Result<List<String>> =
        api.decisions().map { it.toStringListLenient() }

    override suspend fun getInterviewResults(termId: Int): Result<List<InterviewResult>> =
        api.results(termId).decodeList("获取面试结果失败")

    override suspend fun saveInterviewResult(
        applicationId: Int,
        existingResultId: Int?,
        decision: InterviewDecision
    ): Result<Unit> = if (existingResultId == null || existingResultId == 0) {
        api.createResult(
            CreateInterviewResultRequest(
                applicationId = applicationId,
                decision = decision.decision,
                resultDepartmentId = decision.resultDepartmentId,
                resultRoleId = decision.resultRoleId,
                remark = decision.remark
            )
        )
    } else {
        api.updateResult(
            UpdateInterviewResultRequest(
                interviewResultId = existingResultId,
                decision = decision.decision,
                resultDepartmentId = decision.resultDepartmentId,
                resultRoleId = decision.resultRoleId,
                remark = decision.remark
            )
        )
    }

    override suspend fun getExportFields(): Result<ExportFieldsConfig> =
        api.exportFields()

    override suspend fun exportInterviewResults(
        termId: Int,
        departmentId: Int?,
        hasResult: Boolean?,
        fieldsBits: String?
    ): Result<ExportedSpreadsheet> = api.exportResults(
        termId = termId,
        departmentId = departmentId,
        hasResult = hasResult,
        fieldsBits = fieldsBits
    )

    private inline fun <reified T> Result<JsonElement?>.decodeList(failureMessage: String): Result<List<T>> {
        val element = getOrElse { return Result.failure(it) }
        return runCatching { json.decodeListLenient<T>(element) }
            .fold(
                onSuccess = { Result.success(it) },
                onFailure = { appFailure(AppError.Parse("$failureMessage：数据格式与预期不符")) }
            )
    }
}
