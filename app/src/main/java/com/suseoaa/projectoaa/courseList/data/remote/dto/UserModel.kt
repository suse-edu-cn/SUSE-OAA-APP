package com.suseoaa.projectoaa.courseList.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    @SerialName("monitor")
    val monitor: Boolean? = null,
    @SerialName("roleCount")
    val roleCount: Int? = null,
    @SerialName("roleKeys")
    val roleKeys: String? = null,
    @SerialName("roleValues")
    val roleValues: String? = null,
    @SerialName("status")
    val status: Int? = null,
    @SerialName("usable")
    val usable: Boolean? = null
)