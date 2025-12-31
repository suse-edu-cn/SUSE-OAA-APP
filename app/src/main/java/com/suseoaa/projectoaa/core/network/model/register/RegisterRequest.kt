package com.suseoaa.projectoaa.core.network.model.register

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class RegisterRequest(
    @SerialName("name")
    val name: String,
    @SerialName("password")
    val password: String,
    @SerialName("student_id")
    val studentId: String,
    @SerialName("username")
    val username: String
)