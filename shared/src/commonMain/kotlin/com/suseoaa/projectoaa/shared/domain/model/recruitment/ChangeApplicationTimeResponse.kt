package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeApplicationTimeResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: DataX,
    @SerialName("message")
    val message: String
)