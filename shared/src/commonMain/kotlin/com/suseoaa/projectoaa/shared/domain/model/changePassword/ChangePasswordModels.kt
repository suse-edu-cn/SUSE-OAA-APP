package com.suseoaa.projectoaa.shared.domain.model.changePassword

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    @SerialName("oldPassword")
    val oldPassword: String,
    @SerialName("newPassword")
    val newPassword: String,
    @SerialName("confirmPassword")
    val confirmPassword: String
)

@Serializable
data class ChangePasswordResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)
