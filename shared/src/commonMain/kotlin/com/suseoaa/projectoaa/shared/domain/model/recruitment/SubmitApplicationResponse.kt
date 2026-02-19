package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitApplicationResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: String? = null,
    @SerialName("endtime")
    val endtime: String,
    @SerialName("message")
    val message: String,
    @SerialName("starttime")
    val starttime: String
)