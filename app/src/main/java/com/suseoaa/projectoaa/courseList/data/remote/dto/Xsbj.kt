package com.suseoaa.projectoaa.courseList.data.remote.dto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class Xsbj(
    @Json(name = "xslxbj") val xslxbj: String?,
    @Json(name = "xsmc") val xsmc: String?,
    @Json(name = "xsdm") val xsdm: String?
)