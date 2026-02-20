package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: PersonData? = null
)

@Serializable
data class PersonData(
    @SerialName("avatar") val avatar: String = "",
    @SerialName("department") val department: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("email") val email: String = ""
)

@Serializable
data class UpdateUserRequest(
    @SerialName("username") val username: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String
)

@Serializable
data class UpdateAvatarRequest(
    @SerialName("avatar") val avatar: String
)

@Serializable
data class UpdatePersonResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: String? = null
)

@Serializable
data class UploadAvatarResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: String? = null
)
