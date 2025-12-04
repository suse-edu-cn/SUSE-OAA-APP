package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对应 API /match/getDetail 的 data 部分
 */
@Serializable
data class MatchDetail(
    val title: String,
    val author: Author,
    val status: Int,  // API 新增：状态码 0-4

    @SerialName("reg_time")
    val regTime: List<String>,

    @SerialName("match_time")
    val matchTime: List<String>, // 统一命名

    val content: String
)


@Serializable
data class Author(
    val id: Long,
    val name: String
)

/**
 * UI 使用的模型
 */
data class MatchDetailUiItem(
    val id: Int,
    val title: String,
    val organizerName: String,
    val regTime: List<String>,
    val matchTime: List<String>,
    val content: String,
    val status: MatchStatus
)