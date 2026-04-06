package com.suseoaa.projectoaa.shared.domain.model.changePassword

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CaptchaRequest(
    @SerialName("account") val account: String
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("account") val account: String,
    @SerialName("password") val newPassword: String,
    @SerialName("code") val emailCode: String
)

@Serializable
data class ChangePasswordResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: String? = null
)
