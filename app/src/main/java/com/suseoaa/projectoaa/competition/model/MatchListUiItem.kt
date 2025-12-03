package com.suseoaa.projectoaa.competition.model

/**
 * 专门为 MatchListScreen 的 UI 准备的数据类
 */
data class MatchListUiItem(
    val id: Int,
    val title: String,
    val regTime: List<String>,
    val matchTime: List<String>,
    val status: MatchStatus
)