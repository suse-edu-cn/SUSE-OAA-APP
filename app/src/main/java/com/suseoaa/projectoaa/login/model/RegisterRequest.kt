package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val student_id: String,
    val name: String,
    val username: String,
    val password: String,
    val role: String
)