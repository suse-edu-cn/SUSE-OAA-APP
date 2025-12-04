package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchItem(
    val id: Int,
    val title: String,
    val description: String,

    @SerialName("reg_time")
    val regTime: List<String>,

    @SerialName("match_time")
    val matchTime: List<String>,
    val status: Int
)