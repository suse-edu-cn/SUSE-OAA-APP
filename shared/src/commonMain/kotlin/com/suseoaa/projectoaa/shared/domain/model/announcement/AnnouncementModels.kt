package com.suseoaa.projectoaa.shared.domain.model.announcement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val data: AnnouncementData? = null,
    @SerialName("message")
    val message: String
)

@Serializable
data class AnnouncementData(
    @SerialName("data")
    val data: String = "",
    @SerialName("department")
    val department: String = ""
)

@Serializable
data class UpdateAnnouncementInfoRequest(
    @SerialName("department")
    val department: String,
    @SerialName("data")
    val data: String
)

@Serializable
data class UpdateAnnouncementInfoResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)
