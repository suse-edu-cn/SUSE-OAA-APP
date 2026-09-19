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

/**
 * 原先声明在 data/repository 包里，导致 UI 层必须 import 数据层才能拿到模型。
 * 它是纯领域模型，归位到 domain/model。
 */
/**
 * 消息缓存实体类 (用于UI层)
 */
data class MessageCacheEntity(
    val id: Long = 0,
    val studentId: String,
    val content: String,
    val date: Long = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds(),
    val contentHash: String? = null,
    val aiSummary: String? = null
)
