package com.suseoaa.projectoaa.core.network.model.login

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class LoginResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: Data? = null,
    @SerialName("message")
    val message: String
) {
    @Serializable
    data class Data(
        @SerialName("token")
        val token: String
    )
}