package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetApplicationResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: List<Data>,
    @SerialName("endtime")
    val endtime: String,
    @SerialName("message")
    val message: String,
    @SerialName("starttime")
    val starttime: String
)