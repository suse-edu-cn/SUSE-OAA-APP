package com.suseoaa.projectoaa.shared.domain.model.recruitment

import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /interviewer/list 的单条面试官记录。
 *
 * 兼容后端可能返回 `interviewer_id` 或 `id` 作为主键，及 `department_name` / `department`，其余字段均设为可选。
 */
@Serializable
data class Interviewer(
    @SerialName("interviewer_id") val interviewerId: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("term_id") val termId: Int = 0,
    @SerialName("year") val year: Int = 0,
    @SerialName("term_type") val termType: String = "",
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("department_name") val departmentName: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("grade") val grade: Int? = null,
    @SerialName("is_graduated") val isGraduated: Boolean? = null,
    @SerialName("remark") val remark: String = "",
    @SerialName("avatar") val avatar: StoredFile = StoredFile(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val resolvedId: Int get() = if (interviewerId != 0) interviewerId else id
    val resolvedDepartment: String get() = departmentName.ifBlank { department }
    val displayName: String get() = name.ifBlank { username }.ifBlank { remark }.ifBlank { "成员 #$userId" }
}

@Serializable
data class InterviewerEntry(
    @SerialName("user_id") val userId: Int,
    @SerialName("remark") val remark: String
)

/** POST /interviewer/create，一次为某个周期批量指定面试官。 */
@Serializable
data class CreateInterviewersRequest(
    @SerialName("term_id") val termId: Int,
    @SerialName("interviewers") val interviewers: List<InterviewerEntry>
)

@Serializable
data class UpdateInterviewerRequest(
    @SerialName("interviewer_id") val interviewerId: Int,
    @SerialName("remark") val remark: String
)

@Serializable
data class InterviewerIdRequest(
    @SerialName("interviewer_id") val interviewerId: Int
)

/**
 * 面试结果。
 *
 * GET /interviewer/result/list 文档没给响应结构，字段按创建 / 更新接口请求体及线上真实返回结构声明，
 * 全部可选。
 */
@Serializable
data class InterviewResult(
    @SerialName("interview_result_id") val interviewResultId: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("application_id") val applicationId: Int = 0,
    @SerialName("term_id") val termId: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("decision") val decision: String = "",
    @SerialName("result_department_id") val resultDepartmentId: Int = 0,
    @SerialName("result_role_id") val resultRoleId: Int = 0,
    @SerialName("remark") val remark: String = "",
    @SerialName("interviewer_id") val interviewerId: Int = 0,
    @SerialName("operator_user_id") val operatorUserId: Int = 0,
    @SerialName("operator_name") val operatorName: String = "",
    @SerialName("executed_at") val executedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val resolvedId: Int get() = if (interviewResultId != 0) interviewResultId else id
}

/** 录入面试结论时界面提交的内容。 */
data class InterviewDecision(
    val decision: String,
    val resultDepartmentId: Int,
    val resultRoleId: Int,
    val remark: String
)

/** 毕业申请人只允许淘汰类结论，待定不等同于淘汰。 */
object InterviewDecisionRules {
    fun isRejection(decision: String): Boolean =
        listOf("未通过", "不通过", "淘汰", "拒绝", "不录取", "未录取").any { it in decision }

    fun isNonAdmission(decision: String): Boolean = isRejection(decision) || "待定" in decision

    fun isAllowed(isGraduated: Boolean?, decision: String): Boolean =
        decision.isNotBlank() && (isGraduated != true || isRejection(decision))
}

@Serializable
data class CreateInterviewResultRequest(
    @SerialName("application_id") val applicationId: Int,
    @SerialName("decision") val decision: String,
    @SerialName("result_department_id") val resultDepartmentId: Int,
    @SerialName("result_role_id") val resultRoleId: Int,
    @SerialName("remark") val remark: String
)

@Serializable
data class UpdateInterviewResultRequest(
    @SerialName("interview_result_id") val interviewResultId: Int,
    @SerialName("decision") val decision: String,
    @SerialName("result_department_id") val resultDepartmentId: Int,
    @SerialName("result_role_id") val resultRoleId: Int,
    @SerialName("remark") val remark: String
)

/** GET /interviewer/result/fields 的单个可导出字段。 */
@Serializable
data class ExportFieldOption(
    @SerialName("index") val index: Int = 0,
    @SerialName("label") val label: String = ""
)

/** GET /interviewer/result/fields 返回的可导出字段配置与位串模板。 */
@Serializable
data class ExportFieldsConfig(
    @SerialName("total_fields") val totalFields: Int = 0,
    @SerialName("all_bits") val allBits: String = "",
    @SerialName("fields") val fields: List<ExportFieldOption> = emptyList()
) {
    /** 将选中的字段下标集合编码为 '0'/'1' 位串（传入 /interviewer/result/export 的 fields 参数）。 */
    fun buildBits(selectedIndices: Set<Int>): String {
        val count = maxOf(totalFields, allBits.length, fields.maxOfOrNull { it.index + 1 } ?: 0)
        if (count <= 0) return ""
        return buildString(count) {
            for (index in 0 until count) {
                append(if (index in selectedIndices) '1' else '0')
            }
        }
    }
}

/** GET /interviewer/result/export 返回的二进制表格文件。 */
data class ExportedSpreadsheet(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExportedSpreadsheet) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
