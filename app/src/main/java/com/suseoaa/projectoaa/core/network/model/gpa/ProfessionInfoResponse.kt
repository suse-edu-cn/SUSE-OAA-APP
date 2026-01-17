package com.suseoaa.projectoaa.core.network.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfessionInfoResponse(
    val items: List<ProfessionItem>? = null
)

@Serializable
data class ProfessionItem(
    // 培养计划ID
    @SerialName("jxzxjhxx_id") val planId: String? = null,
    @SerialName("jg_id") val jgId: String? = null,
    @SerialName("zyh_id") val zyhId: String? = null,
    @SerialName("njdm") val grade: String? = null
)