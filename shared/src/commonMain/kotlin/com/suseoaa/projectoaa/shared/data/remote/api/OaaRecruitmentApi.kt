package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateApplicationRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateInterviewResultRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateInterviewersRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.CreateTermRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldsConfig
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportedSpreadsheet
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewerIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermIdRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateApplicationRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateInterviewResultRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateInterviewerRequest
import com.suseoaa.projectoaa.shared.domain.model.recruitment.UpdateTermRequest
import kotlinx.serialization.json.JsonElement

/**
 * `/term`、`/application`、`/interviewer` 下的接口：招新换届。
 *
 * 文档里有好几个接口没写响应结构（申请列表、可填写的部门 / 职位、面试结果、决定列表），
 * 这些先按 [JsonElement] 取回，由 Repository 做宽松解析。
 */
class OaaRecruitmentApi(private val requester: OaaRequester) {

    // ==================== 周期 ====================

    suspend fun terms(year: Int?, type: String?): Result<List<Term>?> =
        requester.get("/term/list", "获取招新周期失败", params = mapOf("year" to year, "type" to type))

    suspend fun createTerm(request: CreateTermRequest): Result<Unit> =
        requester.post("/term/create", request, "创建周期失败")

    suspend fun updateTerm(request: UpdateTermRequest): Result<Unit> =
        requester.post("/term/update", request, "更新周期失败")

    suspend fun deleteTerm(request: TermIdRequest): Result<Unit> =
        requester.post("/term/delete", request, "删除周期失败")

    // ==================== 申请 ====================

    suspend fun createApplication(request: CreateApplicationRequest): Result<Unit> =
        requester.post("/application/create", request, "提交申请失败")

    suspend fun updateApplication(request: UpdateApplicationRequest): Result<Unit> =
        requester.post("/application/update", request, "更新申请失败")

    suspend fun myApplications(): Result<JsonElement?> =
        requester.get("/application/me", "获取我的申请失败")

    suspend fun deleteApplication(request: ApplicationIdRequest): Result<Unit> =
        requester.post("/application/delete", request, "删除申请失败")

    /** [departmentId] 为 null 时后端默认查面试官所属部门。 */
    suspend fun applications(
        termId: Int?,
        departmentId: Int?,
        hasResult: Boolean? = null,
        name: String? = null
    ): Result<JsonElement?> =
        requester.get(
            "/application/list",
            "获取申请列表失败",
            params = mapOf(
                "term_id" to termId,
                "department_id" to departmentId,
                "has_result" to hasResult,
                "name" to name?.trim()?.ifBlank { null }
            )
        )

    suspend fun fillableRoles(departmentId: Int): Result<JsonElement?> =
        requester.get("/application/role", "获取可申请职位失败", params = mapOf("department_id" to departmentId))

    suspend fun fillableDepartments(roleId: Int): Result<JsonElement?> =
        requester.get("/application/department", "获取可申请部门失败", params = mapOf("role_id" to roleId))

    // ==================== 面试 ====================

    suspend fun interviewers(termId: Int): Result<JsonElement?> =
        requester.get("/interviewer/list", "获取面试官列表失败", params = mapOf("term_id" to termId))

    suspend fun createInterviewers(request: CreateInterviewersRequest): Result<Unit> =
        requester.post("/interviewer/create", request, "添加面试官失败")

    suspend fun updateInterviewer(request: UpdateInterviewerRequest): Result<Unit> =
        requester.post("/interviewer/update", request, "更新面试官失败")

    suspend fun deleteInterviewer(request: InterviewerIdRequest): Result<Unit> =
        requester.post("/interviewer/delete", request, "删除面试官失败")

    suspend fun createResult(request: CreateInterviewResultRequest): Result<Unit> =
        requester.post("/interviewer/result/create", request, "录入面试结果失败")

    suspend fun updateResult(request: UpdateInterviewResultRequest): Result<Unit> =
        requester.post("/interviewer/result/update", request, "更新面试结果失败")

    suspend fun results(termId: Int?): Result<JsonElement?> =
        requester.get("/interviewer/result/list", "获取面试结果失败", params = mapOf("term_id" to termId))

    suspend fun decisions(): Result<JsonElement?> =
        requester.get("/interviewer/result/decision", "获取面试结论选项失败")

    suspend fun exportFields(): Result<ExportFieldsConfig> =
        requester.get("/interviewer/result/fields", "获取导出字段列表失败")

    suspend fun exportResults(
        termId: Int,
        departmentId: Int? = null,
        hasResult: Boolean? = null,
        fieldsBits: String? = null
    ): Result<ExportedSpreadsheet> = requester.getBinary(
        path = "/interviewer/result/export",
        fallbackMessage = "导出面试结果失败",
        params = mapOf(
            "term_id" to termId,
            "department_id" to departmentId,
            "has_result" to hasResult,
            "fields" to fieldsBits?.ifBlank { null }
        )
    ).map { payload ->
        ExportedSpreadsheet(
            fileName = payload.fileName,
            mimeType = payload.contentType,
            bytes = payload.bytes
        )
    }
}
