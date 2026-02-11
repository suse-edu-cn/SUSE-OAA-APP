package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeApplicationSubmitTime(
    @SerialName("endtime")
    val endtime: String,
    @SerialName("starttime")
    val starttime: String
)