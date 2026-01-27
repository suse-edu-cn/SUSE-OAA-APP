package com.suseoaa.projectoaa.shared.domain.model.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)
