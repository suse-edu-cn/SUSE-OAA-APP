package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserQueryRequest(
    @SerialName("department") val department: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("role") val role: String = ""
)

@Serializable
data class UserQueryResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: List<UserQueryData> = emptyList()
)

@Serializable
data class UserQueryData(
    @SerialName("id") val id: Int = -1,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("email") val email: String = ""
)

@Serializable
data class UserChangeMessageResponse(
    @SerialName("code") val code: Int = 200,
    @SerialName("message") val message: String = ""
)
