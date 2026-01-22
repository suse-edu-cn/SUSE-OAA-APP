package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import kotlinx.serialization.json.JsonElement

/**
{
"code": 500,
"data": null,
"endtime": "2026-01-29 00:00:00",
"message": "不在查询时间范围内",
"starttime": "2026-01-22 00:00:00"
}
 */
@Keep
@Serializable
data class GetApplicationOutTimeResponse(
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