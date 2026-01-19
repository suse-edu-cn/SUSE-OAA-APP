package com.suseoaa.projectoaa.core.network.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class StringDataResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: String? = null
)
