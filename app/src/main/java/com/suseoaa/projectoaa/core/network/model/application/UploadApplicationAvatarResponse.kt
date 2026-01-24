package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadApplicationAvatarResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: String,
    @SerialName("message")
    val message: String
)