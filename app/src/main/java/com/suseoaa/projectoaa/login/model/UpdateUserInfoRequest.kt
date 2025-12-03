package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserInfoRequest(
    val student_id: Long,
    val name: String,
    val department: String,
    val role: String
)