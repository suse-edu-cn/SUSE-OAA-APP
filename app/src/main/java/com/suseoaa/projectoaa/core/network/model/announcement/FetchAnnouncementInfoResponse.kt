package com.suseoaa.projectoaa.core.network.model.announcement


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
"code": 200,
"message": "获取成功",
"data": {
"department": "协会",
"data": "协会介绍信息,测试"
}
}
 */
@Keep
@Serializable
data class FetchAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: Data,
    @SerialName("message")
    val message: String
) {
    @Keep
    @Serializable
    data class Data(
        @SerialName("data")
        val `data`: String,
        @SerialName("department")
        val department: String
    )
}