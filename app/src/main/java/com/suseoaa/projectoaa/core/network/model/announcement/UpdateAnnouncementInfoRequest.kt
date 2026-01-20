package com.suseoaa.projectoaa.core.network.model.announcement


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
"updateinfo":"协会介绍信息,测试",
"department":"协会"
}
 */
@Keep
@Serializable
data class UpdateAnnouncementInfoRequest(
    @SerialName("department")
    val department: String,
    @SerialName("updateinfo")
    val updateinfo: String
)