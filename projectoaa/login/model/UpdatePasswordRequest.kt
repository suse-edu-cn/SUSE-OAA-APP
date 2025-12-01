package com.suseoaa.projectoaa.login.model

data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)