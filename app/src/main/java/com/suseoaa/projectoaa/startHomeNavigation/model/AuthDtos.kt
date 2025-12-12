package com.suseoaa.projectoaa.startHomeNavigation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoResponse(
    @SerialName("student_id") val studentId: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)