package com.suseoaa.projectoaa.shared.domain.model.changePassword

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    @SerialName("oldpassword") val oldPassword: String,
    @SerialName("password") val newPassword: String,
    @SerialName("code") val emailCode: String
)

@Serializable
data class ChangePasswordResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: String? = null
)
