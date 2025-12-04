package com.suseoaa.projectoaa.login.model
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int = -1,
    val message: String = "",
    val data: T? = null
)