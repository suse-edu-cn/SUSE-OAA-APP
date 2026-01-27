package com.suseoaa.projectoaa.shared.domain.model.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcademicMessageResponse(
    @SerialName("items")
    val items: List<AcademicMessageItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0
)

@Serializable
data class AcademicMessageItem(
    @SerialName("bt")
    val bt: String? = "",         // 标题
    @SerialName("nr")
    val nr: String? = "",         // 内容
    @SerialName("cjsj")
    val cjsj: String? = "",       // 创建时间
    @SerialName("xxlx")
    val xxlx: String? = "",       // 消息类型
    @SerialName("fbr")
    val fbr: String? = "",        // 发布人
    @SerialName("kcmc")
    val kcmc: String? = ""        // 课程名称
)
