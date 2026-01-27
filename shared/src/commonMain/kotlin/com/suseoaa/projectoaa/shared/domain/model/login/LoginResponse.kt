package com.suseoaa.projectoaa.shared.domain.model.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
