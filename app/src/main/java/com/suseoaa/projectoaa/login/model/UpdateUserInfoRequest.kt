package com.suseoaa.projectoaa.login.model

data class UpdateUserInfoRequest(
    val studentid: Long,
    val name: String,
    val department: String,
    val role: String
)