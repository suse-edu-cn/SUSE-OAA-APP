package com.suseoaa.projectoaa.core.network.model.person


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: Data,
    @SerialName("message")
    val message: String
)