package com.suseoaa.projectoaa.core.network.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    @SerialName("username")
    val username: String,
    @SerialName("name")
    val name: String
)
