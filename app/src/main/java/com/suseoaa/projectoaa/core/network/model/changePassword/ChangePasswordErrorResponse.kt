package com.suseoaa.projectoaa.core.network.model.changePassword


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
"code": 400,
"message": "旧密码错误",
"data": null
}
 */
@Keep
@Serializable
data class ChangePasswordErrorResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: String? = null,
    @SerialName("message")
    val message: String
)