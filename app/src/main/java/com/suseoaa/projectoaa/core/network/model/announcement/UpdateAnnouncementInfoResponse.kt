package com.suseoaa.projectoaa.core.network.model.announcement


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
"code": 200,
"message": "更新成功",
"data": "协会介绍信息,测试"
}
 */
@Keep
@Serializable
data class UpdateAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: String,
    @SerialName("message")
    val message: String
)