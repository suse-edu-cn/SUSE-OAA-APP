package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val studentid: String,
    val name: String,
    val username: String,
    val password: String,
    val role: String
)