package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import kotlinx.serialization.json.JsonElement

/**
{
"code": 200,
"data": null,
"endtime": "2026-01-29 00:00:00",
"message": "提交成功",
"starttime": "2026-01-22 00:00:00"
}
 */
@Keep
@Serializable
data class SubmitApplicationResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: JsonElement? = null,
    @SerialName("endtime")
    val endtime: String,
    @SerialName("message")
    val message: String,
    @SerialName("starttime")
    val starttime: String
)