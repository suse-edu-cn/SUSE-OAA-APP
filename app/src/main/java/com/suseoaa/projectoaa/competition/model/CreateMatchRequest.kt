package com.suseoaa.projectoaa.competition.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateMatchRequest(
    val title: String,
    val content: String,
    val description: String,
    @SerialName("start_at")
    val startAt: String,
    @SerialName("end_at")
    val endAt: String,
    @SerialName("reg_start_at")
    val regStartAt: String,
    @SerialName("reg_end_at")
    val regEndAt: String
)