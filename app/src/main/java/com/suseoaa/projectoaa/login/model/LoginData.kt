package com.suseoaa.projectoaa.login.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val token: String?
)