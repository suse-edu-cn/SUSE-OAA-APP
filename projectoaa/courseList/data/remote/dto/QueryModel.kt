package com.suseoaa.projectoaa.courseList.data.remote.dto


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QueryModel(
    @Json(name = "currentPage")
    val currentPage: Int?,
    @Json(name = "currentResult")
    val currentResult: Int?,
    @Json(name = "entityOrField")
    val entityOrField: Boolean?,
    @Json(name = "limit")
    val limit: Int?,
    @Json(name = "offset")
    val offset: Int?,
    @Json(name = "pageNo")
    val pageNo: Int?,
    @Json(name = "pageSize")
    val pageSize: Int?,
    @Json(name = "showCount")
    val showCount: Int?,
    @Json(name = "sorts")
    val sorts: List<Any?>?,
    @Json(name = "totalCount")
    val totalCount: Int?,
    @Json(name = "totalPage")
    val totalPage: Int?,
    @Json(name = "totalResult")
    val totalResult: Int?
)