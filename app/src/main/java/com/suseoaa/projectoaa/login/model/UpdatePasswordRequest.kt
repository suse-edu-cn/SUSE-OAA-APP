package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)