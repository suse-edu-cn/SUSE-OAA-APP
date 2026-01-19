package com.suseoaa.projectoaa.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 通用泛型响应类
 * @param T data 字段的具体类型 (如 Data, String, 或其他)
 */
@Serializable
data class BaseResponse<T>(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: T? = null
)