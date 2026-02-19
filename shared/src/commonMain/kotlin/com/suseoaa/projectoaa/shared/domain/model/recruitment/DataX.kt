package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataX(
    @SerialName("endtime")
    val endtime: String,
    @SerialName("starttime")
    val starttime: String
)