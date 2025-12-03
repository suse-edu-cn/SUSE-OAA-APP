package com.suseoaa.projectoaa.courseList.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Xsbj(
    @SerialName("xslxbj") val xslxbj: String? = null,
    @SerialName("xsmc") val xsmc: String? = null,
    @SerialName("xsdm") val xsdm: String? = null
)