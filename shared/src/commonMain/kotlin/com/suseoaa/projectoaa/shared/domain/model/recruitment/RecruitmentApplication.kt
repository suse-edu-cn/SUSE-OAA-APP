package com.suseoaa.projectoaa.shared.domain.model.recruitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecruitmentApplication(
    val id: Int = 0,
    val name: String = "",
    val reason: String = "",
    val choice1: String = "",
    val choice2: String = "",
    val experience: String = "",
    val phone: String = "",
    val gender: String = "",
    val major: String = "",
    @SerialName("class") val className: String = "",
    val birthday: String = "",
    val qq: String = "",
    val politic_stance: String = "",
    val adjustment: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("studentid") val studentIdCompat: String = "",
    val avator: String = "",
    val avatar: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val status: String = "",
    val role1: String = "",
    val role2: String = ""
) {
    val resolvedStudentId: String
        get() = studentId.ifBlank { studentIdCompat }

    val avatarUrl: String
        get() = avator.ifBlank { avatar }
}

@Serializable
data class RecruitmentSubmitRequest(
    val reason: String,
    val choice1: String,
    val choice2: String,
    val experience: String,
    val phone: String,
    val gender: String,
    val major: String,
    @SerialName("class") val className: String,
    val birthday: String,
    val qq: String,
    val politic_stance: String,
    @SerialName("adjustiment") val adjustiment: Int,
    val role1: String,
    val role2: String
)

@Serializable
data class RecruitmentTimeWindow(
    val starttime: String = "",
    val endtime: String = ""
)

@Serializable
data class RecruitmentResponse<T>(
    val code: Int,
    val data: T?,
    val message: String,
    val starttime: String? = null,
    val endtime: String? = null
)

@Serializable
data class ChangeStatusRequest(
    val studentid: List<String>,
    val status: List<String>
)

@Serializable
data class ChangeTimeRequest(
    val starttime: String,
    val endtime: String
)

fun RecruitmentApplication.toSubmitRequest(): RecruitmentSubmitRequest {
    return RecruitmentSubmitRequest(
        reason = reason,
        choice1 = choice1,
        choice2 = choice2,
        experience = experience,
        phone = phone,
        gender = gender,
        major = major,
        className = className,
        birthday = birthday,
        qq = qq,
        politic_stance = politic_stance,
        adjustiment = adjustment,
        role1 = role1,
        role2 = role2
    )
}
