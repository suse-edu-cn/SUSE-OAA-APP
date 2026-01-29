package com.suseoaa.projectoaa.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== Login API ====================
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val data: LoginData? = null,
    @SerialName("message")
    val message: String
)

@Serializable
data class LoginData(
    @SerialName("token")
    val token: String
)

// ==================== Register API ====================
@Serializable
data class RegisterRequest(
    @SerialName("name")
    val name: String,
    @SerialName("password")
    val password: String,
    @SerialName("student_id")
    val studentId: String,
    @SerialName("username")
    val username: String
)

@Serializable
data class RegisterResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val data: RegisterData? = null,
    @SerialName("message")
    val message: String
)

@Serializable
class RegisterData

// ==================== Person API ====================
@Serializable
data class PersonResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: PersonData? = null
)

@Serializable
data class PersonData(
    @SerialName("avatar")
    val avatar: String = "",
    @SerialName("department")
    val department: String? = null,
    @SerialName("name")
    val name: String = "",
    @SerialName("role")
    val role: String = "",
    @SerialName("student_id")
    val studentId: String = "",
    @SerialName("username")
    val username: String = ""
)

@Serializable
data class UpdateUserRequest(
    @SerialName("username")
    val username: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class UpdateAvatarRequest(
    @SerialName("avatar")
    val avatar: String
)

@Serializable
data class UpdatePersonResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: String? = null
)

@Serializable
data class UploadAvatarResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: String? = null
)

// ==================== Change Password API ====================
@Serializable
data class ChangePasswordRequest(
    @SerialName("old_password")
    val oldPassword: String,
    @SerialName("new_password")
    val newPassword: String
)

@Serializable
data class ChangePasswordResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: String? = null
)

// ==================== Announcement API ====================
@Serializable
data class FetchAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val data: AnnouncementData,
    @SerialName("message")
    val message: String
)

@Serializable
data class AnnouncementData(
    @SerialName("data")
    val data: String,
    @SerialName("department")
    val department: String
)

@Serializable
data class UpdateAnnouncementInfoRequest(
    @SerialName("department")
    val department: String,
    @SerialName("updateinfo")
    val updateinfo: String
)

@Serializable
data class UpdateAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val data: String,
    @SerialName("message")
    val message: String
)

// ==================== Base Response ====================
@Serializable
data class BaseResponse<T>(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: T? = null
)

// ==================== GPA 培养计划 API ====================
/**
 * 专业列表项
 */
@Serializable
data class MajorItem(
    @SerialName("id")
    val majorId: String = "",      // 专业ID (zyh_id)
    @SerialName("zymc")
    val majorName: String = ""     // 专业名称
)

/**
 * 培养计划信息响应
 */
@Serializable
data class ProfessionInfoResponse(
    @SerialName("items")
    val items: List<PlanInfo>? = null
)

@Serializable
data class PlanInfo(
    @SerialName("jxzxjhxx_id")
    val planId: String = ""        // 培养计划ID
)

/**
 * 培养计划课程列表响应
 */
@Serializable
data class TeachingPlanResponse(
    @SerialName("items")
    val items: List<TeachingPlanItem>? = null
)

@Serializable
data class TeachingPlanItem(
    @SerialName("kch")
    val courseNumber: String? = null,      // 课程号
    @SerialName("kcmc")
    val courseName: String? = null,        // 课程名
    @SerialName("xf")
    val credit: String? = null,            // 学分
    @SerialName("zyzgkcbj")
    val degreeCourseFlag: String? = null,  // 学位课程标记 ("是"/"否")
    @SerialName("kcxzmc")
    val courseNature: String? = null       // 课程性质
)
