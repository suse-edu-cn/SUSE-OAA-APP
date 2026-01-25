package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataXX(
    @SerialName("endtime")
    val endtime: String,
    @SerialName("starttime")
    val starttime: String
)