package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserInfoRequest(
    val studentid: Long,
    val name: String,
    val department: String,
    val role: String
)