package com.suseoaa.projectoaa.core.network.model.person


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("avatar")
    val avatar: String,
    @SerialName("department")
    val department: String?,
    @SerialName("name")
    val name: String,
    @SerialName("role")
    val role: String,
    @SerialName("student_id")
    val studentId: String,
    @SerialName("username")
    val username: String
)