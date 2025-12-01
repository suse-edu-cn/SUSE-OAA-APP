package com.suseoaa.projectoaa.courseList.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RSAKey(
    val modulus: String,
    val exponent: String
)