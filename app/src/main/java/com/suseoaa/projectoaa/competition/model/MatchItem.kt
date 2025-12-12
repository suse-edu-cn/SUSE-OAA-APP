package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchItem(
    val id: Int,
    val title: String,
    val description: String? = null,

    @SerialName("reg_time")
    val regTime: List<String> = emptyList(),

    @SerialName("match_time")
    val matchTime: List<String> = emptyList(),
    val status: Int
)