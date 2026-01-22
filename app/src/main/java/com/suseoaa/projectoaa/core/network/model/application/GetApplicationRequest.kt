package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
    "role":"",
    "department":"组织宣传部"
}
*/
@Keep
@Serializable
data class GetApplicationRequest(
    @SerialName("department")
    val department: String,
    @SerialName("role")
    val role: String
)