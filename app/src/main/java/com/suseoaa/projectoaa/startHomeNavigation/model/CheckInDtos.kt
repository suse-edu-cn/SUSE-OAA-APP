package com.suseoaa.projectoaa.startHomeNavigation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Request DTOs (发给后端的) ---

@Serializable
data class CheckInRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("date") val date: String
)

// --- Response DTOs (从后端收到的) ---

@Serializable
data class CheckInStatusResponse(
    @SerialName("is_checked_in") val isCheckedIn: Boolean,
    @SerialName("count") val checkInCount: Int,
    @SerialName("last_check_in_date") val lastDate: String? = null
)