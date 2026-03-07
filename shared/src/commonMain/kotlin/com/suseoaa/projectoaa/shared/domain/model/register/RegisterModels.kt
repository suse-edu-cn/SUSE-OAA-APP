package com.suseoaa.projectoaa.shared.domain.model.register

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("name") val name: String,
    @SerialName("password") val password: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String
)

@Serializable
data class RegisterResponse(
    @SerialName("code") val code: Int,
    @SerialName("data") val data: RegisterData? = null,
    @SerialName("message") val message: String = ""
)

@Serializable
class RegisterData
