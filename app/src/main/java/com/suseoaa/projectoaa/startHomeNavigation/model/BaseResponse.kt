package com.suseoaa.projectoaa.startHomeNavigation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标准后端响应封装
 * 泛型 T 代表具体的 data 内容
 */
@Serializable
data class BaseResponse<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String?,
    @SerialName("data") val data: T? = null // 默认值设为 null，防止后端不返回 data 字段时报错
) {
    fun isSuccess(): Boolean = code == 200
}