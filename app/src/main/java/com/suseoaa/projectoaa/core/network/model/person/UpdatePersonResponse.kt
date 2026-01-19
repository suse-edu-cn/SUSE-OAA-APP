package com.suseoaa.projectoaa.core.network.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 专门用于修改用户信息的响应
 * 接口返回: {"code": 200, "message": "更新成功", "data": null}
 */
@Serializable
data class UpdatePersonResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: String? = null
)