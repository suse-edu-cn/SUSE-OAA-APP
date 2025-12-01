package com.suseoaa.projectoaa.courseList.data.remote.dto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class UserModel(
    @Json(name = "monitor")
    val monitor: Boolean?,
    @Json(name = "roleCount")
    val roleCount: Int?,
    @Json(name = "roleKeys")
    val roleKeys: String?,
    @Json(name = "roleValues")
    val roleValues: String?,
    @Json(name = "status")
    val status: Int?,
    @Json(name = "usable")
    val usable: Boolean?
)