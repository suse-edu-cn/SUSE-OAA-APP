package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchItem(
    val id: Int,
    val title: String,
    val color: String,
    val description: String,
    val regTime: List<String>,
    val matchTime: List<String>
)