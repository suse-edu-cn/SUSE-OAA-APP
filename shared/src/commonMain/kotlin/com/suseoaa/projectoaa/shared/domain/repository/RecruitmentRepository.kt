package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldsConfig
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportedSpreadsheet
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecision
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewResult
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Interviewer
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewerEntry
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Period
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term

/**
 * RecruitmentRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface RecruitmentRepository {

    // ==================== 周期 ====================

    suspend fun getTerms(year: Int? = null, type: String? = null): Result<List<Term>>

    suspend fun createTerm(year: Int, type: String, title: String, editPeriod: Period, queryPeriod: Period): Result<Unit>

    suspend fun updateTerm(termId: Int, title: String, editPeriod: Period, queryPeriod: Period): Result<Unit>

    suspend fun deleteTerm(termId: Int): Result<Unit>

    // ==================== 申请 ====================

    /** 上传申请表个人照片（POST /upload/image，scene = application），返回对象存储 URI 与预览 URL。 */
    suspend fun uploadApplicationAvatar(imageData: ByteArray): Result<StoredFile>

    /** 当前账号提交过的所有申请（可能跨多个周期）。 */
    suspend fun getMyApplications(): Result<List<Application>>

    suspend fun createApplication(termId: Int, form: ApplicationForm): Result<Unit>

    /** [existing] 为该周期已提交的申请，用于补上文档里缺失的定位字段。 */
    suspend fun updateApplication(termId: Int, existing: Application, form: ApplicationForm): Result<Unit>

    suspend fun deleteApplication(applicationId: Int): Result<Unit>

    /** 面试官查看申请；[departmentId] 为 null 时后端默认面试官所属部门。 */
    suspend fun getApplications(
        termId: Int,
        departmentId: Int?,
        hasResult: Boolean? = null,
        name: String? = null
    ): Result<List<Application>>

    /** 某部门可以申请的职位。 */
    suspend fun getFillableRoles(departmentId: Int): Result<List<Role>>

    /** 某职位可以申请的部门。 */
    suspend fun getFillableDepartments(roleId: Int): Result<List<Department>>

    // ==================== 面试 ====================

    suspend fun getInterviewers(termId: Int): Result<List<Interviewer>>

    suspend fun createInterviewers(termId: Int, interviewers: List<InterviewerEntry>): Result<Unit>

    suspend fun updateInterviewer(interviewerId: Int, remark: String): Result<Unit>

    suspend fun deleteInterviewer(interviewerId: Int): Result<Unit>

    /** 后端只接受的面试结论取值。 */
    suspend fun getDecisions(): Result<List<String>>

    suspend fun getInterviewResults(termId: Int): Result<List<InterviewResult>>

    /** 没有 [existingResultId] 时新建，有则更新。 */
    suspend fun saveInterviewResult(
        applicationId: Int,
        existingResultId: Int?,
        decision: InterviewDecision
    ): Result<Unit>

    suspend fun getExportFields(): Result<ExportFieldsConfig>

    suspend fun exportInterviewResults(
        termId: Int,
        departmentId: Int? = null,
        hasResult: Boolean? = null,
        fieldsBits: String? = null
    ): Result<ExportedSpreadsheet>
}
