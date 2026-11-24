package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /user/list 的单条记录。 */
@Serializable
data class UserListItem(
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("grade") val grade: Int? = null,
    @SerialName("is_graduated") val isGraduated: Boolean? = null,
    @SerialName("avatar") val avatar: StoredFile = StoredFile(),
    @SerialName("role_level") val roleLevel: Int? = null,
    @SerialName("is_interviewer") val isInterviewer: Boolean? = null,
    @SerialName("is_deleting") val isDeleting: Boolean = false
) {
    val resolvedGrade: Int? get() = grade?.takeIf(EnrollmentGrade::isValid)
}

@Serializable
data class UserPage(
    @SerialName("total") val total: Int = 0,
    @SerialName("list") val list: List<UserListItem> = emptyList()
)

/** 成员列表查询条件；空串 / null 表示不按该项筛选。[isAll] 为 true 时不分页拉取全部可见成员。 */
data class UserQuery(
    val keyword: String = "",
    val department: String = "",
    val role: String = "",
    val grade: Int? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
    val isAll: Boolean? = null,
    val isGraduated: Boolean? = null
)

/** POST /user/batch 的单条变更。 */
@Serializable
data class UserBatchItem(
    @SerialName("user_id") val userId: Int,
    @SerialName("department_id") val departmentId: Int,
    @SerialName("role_id") val roleId: Int,
    /** 仅管理员修正年级时传递；不支持手动设置毕业状态。 */
    @SerialName("grade") val grade: Int? = null
)

/** /user/batch 返回的失败条目，全部成功时为空数组。 */
@Serializable
data class UserBatchFailure(
    @SerialName("id") val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("error_message") val errorMessage: String = ""
)

/**
 * POST /user/delete，两种用法只能二选一（null 的字段不会被序列化）：
 * - 管理员删除成员：只传 [userId]；
 * - 用户注销自己：只传 [code]，即 scene 为 delete_user 的邮箱验证码，账号随后进入冷静期。
 */
@Serializable
data class DeleteUserRequest(
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("code") val code: String? = null
)

/** POST /user/delete/cancel：冷静期内凭验证码撤销注销。 */
@Serializable
data class CancelDeleteRequest(
    @SerialName("code") val code: String
)
