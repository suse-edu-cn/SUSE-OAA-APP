package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchDetail(
    val title: String,
    val organizer: Organizer,
    val regTime: List<String>,
    val conTime: List<String>,
    val content: String
)

@Serializable
data class Organizer(
    val id: Long,
    val name: String
)