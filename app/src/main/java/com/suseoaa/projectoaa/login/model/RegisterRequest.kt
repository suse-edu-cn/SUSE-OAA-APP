package com.suseoaa.projectoaa.login.model

data class RegisterRequest(
    val studentid: String,
    val name: String,
    val username: String,
    val password: String,
    val role: String
)