package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("avatar")
    val avatar: String? = null
)

@Serializable
data class UpdatePersonResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)

@Serializable
data class UploadAvatarResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: AvatarData? = null
)

@Serializable
data class AvatarData(
    @SerialName("url")
    val url: String? = null
)
