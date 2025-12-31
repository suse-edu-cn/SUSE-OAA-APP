package com.suseoaa.projectoaa.core.network.model.course


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Keep
@Serializable
data class QueryModel(
    @SerialName("currentPage")
    val currentPage: Int? = null,
    @SerialName("currentResult")
    val currentResult: Int? = null,
    @SerialName("entityOrField")
    val entityOrField: Boolean? = null,
    @SerialName("limit")
    val limit: Int? = null,
    @SerialName("offset")
    val offset: Int? = null,
    @SerialName("pageNo")
    val pageNo: Int? = null,
    @SerialName("pageSize")
    val pageSize: Int? = null,
    @SerialName("showCount")
    val showCount: Int? = null,
    @SerialName("sorts")
    val sorts: List<JsonElement>? = null,
    @SerialName("totalCount")
    val totalCount: Int? = null,
    @SerialName("totalPage")
    val totalPage: Int? = null,
    @SerialName("totalResult")
    val totalResult: Int? = null
)
