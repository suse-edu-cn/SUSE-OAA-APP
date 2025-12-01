package com.suseoaa.projectoaa.login.model

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)