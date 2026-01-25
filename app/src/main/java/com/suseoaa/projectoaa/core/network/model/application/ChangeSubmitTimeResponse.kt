package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeSubmitTimeResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: DataXX,
    @SerialName("message")
    val message: String
)