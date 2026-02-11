package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeStatus(
    @SerialName("status")
    val status: List<String>,
    @SerialName("studentid")
    val studentid: List<String>
)