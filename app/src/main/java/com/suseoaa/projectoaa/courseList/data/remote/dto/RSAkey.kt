package com.suseoaa.projectoaa.courseList.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RSAKey(
    val modulus: String,
    val exponent: String
)