package com.suseoaa.projectoaa.shared.domain.model.register

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
    @SerialName("confirmPassword")
    val confirmPassword: String
)

@Serializable
data class RegisterResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: String? = null
)

@Serializable
data class RegisterErrorResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)
