package com.suseoaa.projectoaa.core.network.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 专门用于上传头像的响应
 * 接口返回: {"code": 200, "message": "...", "data": "https://..."}
 */
@Serializable
data class UploadAvatarResponse(
    @SerialName("code")
    val code: Int,

    @SerialName("message")
    val message: String,

    @SerialName("data")
    val data: String? = null
)