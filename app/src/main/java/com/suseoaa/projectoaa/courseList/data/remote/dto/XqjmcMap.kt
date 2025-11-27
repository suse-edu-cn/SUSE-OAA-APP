package com.suseoaa.projectoaa.courseList.data.remote.dto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class XqjmcMap(
    @Json(name = "x1") val x1: String?,
    @Json(name = "x2") val x2: String?,
    @Json(name = "x3") val x3: String?
)